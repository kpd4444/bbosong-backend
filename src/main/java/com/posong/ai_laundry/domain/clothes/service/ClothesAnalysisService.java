package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisAiResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.ai.OpenAiChatOptionsFactory;
import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import com.posong.ai_laundry.global.file.ImageFileSizeExceededException;
import com.posong.ai_laundry.global.file.ImageFileValidator;
import com.posong.ai_laundry.global.resilience.ExternalApiCallTimeoutException;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitBreaker;
import com.posong.ai_laundry.global.resilience.ExternalApiCircuitOpenException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothesAnalysisService {

	private static final String OPENAI_CIRCUIT = "openai";
	private static final Set<String> EXPECTED_OPENAI_EXCEPTIONS = Set.of(
			"GeneralException",
			"ExternalApiCircuitOpenException",
			"ExternalApiCallTimeoutException"
	);

	private static final String ANALYSIS_PROMPT = """
			너는 의류 이미지 분석 전문가다.
			사용자가 업로드한 옷 사진을 보고 세탁 가이드 생성에 필요한 정보를 추출한다.
			사진에서 확인 가능한 시각 정보만 사용하고, 확실하지 않은 내용은 보수적으로 추정한다.

			공통 응답 규칙:
			- 반드시 JSON만 반환한다. JSON 앞뒤에 설명, 마크다운, 코드블록을 붙이지 않는다.
			- 모든 필드는 null이나 빈 문자열 없이 채운다.
			- 사진만으로 확실하지 않은 값은 단정하지 말고 "추정" 또는 "알 수 없음"을 포함해 작성한다.
			- 관리 방법은 안전한 쪽을 우선한다. 확실하지 않으면 고온 세탁, 강한 탈수, 건조기, 표백제 사용을 권하지 않는다.
			- 세탁 라벨을 직접 읽을 수 없는 경우 라벨 확인이 필요하다는 문장을 자연스럽게 포함한다.
			- 사용자가 바로 참고할 수 있게 한국어 문장으로 구체적으로 작성한다.

			필드별 작성 기준:
			- categoryName:
			  상의, 하의, 아우터, 원피스, 치마, 속옷, 신발, 기타 중 하나만 작성한다.
			  분류가 애매하면 가장 안전한 상위 분류를 선택하고, 정말 판단이 어려우면 기타를 사용한다.
			- name:
			  색상이나 형태를 포함해 사용자가 구분하기 쉬운 짧은 이름으로 작성한다.
			  예: 흰색 반팔 티셔츠, 검정 패딩 점퍼, 파란 데님 팬츠
			- material:
			  사진으로 추정 가능한 소재를 작성한다.
			  혼용률을 임의로 만들지 않는다.
			  소재가 불확실하면 "면 소재로 추정", "합성섬유로 추정", "사진만으로 정확한 소재는 알 수 없음"처럼 작성한다.
			- color:
			  가장 넓게 보이는 대표 색상을 한글로 작성한다.
			  여러 색이 뚜렷하면 "흰색과 검정", "파란색 계열"처럼 작성한다.
			- washingMethod:
			  2~3문장으로 작성한다.
			  권장 물 온도, 세탁 코스, 세제 종류, 단독 세탁 여부를 포함한다.
			  소재가 불확실하면 라벨 확인을 전제로 찬물, 약한 코스, 중성세제처럼 안전한 방법을 권장한다.
			- caution:
			  2~3문장으로 작성한다.
			  수축, 이염, 형태 변형, 건조기 사용, 표백제 사용, 직사광선 건조 중 관련 있는 위험을 포함한다.
			  위험만 나열하지 말고 피하는 방법도 함께 작성한다.
			- washRules: 아래 구조로 세탁 관련 판단을 JSON object로 답한다.
			  - waterWash: 물세탁 가능 여부를 true/false/null로 답한다.
			  - maxWaterTemperature: 권장 최대 세탁 온도를 숫자로 답한다. 모르면 null로 답한다.
			  - bleachAllowed: 표백제 사용 가능 여부를 true/false/null로 답한다.
			  - dryerAllowed: 건조기 사용 가능 여부를 true/false/null로 답한다.
			  - ironAllowed: 다림질 가능 여부를 true/false/null로 답한다.
			  - dryCleanAllowed: 드라이클리닝 가능 여부를 true/false/null로 답한다.
			  - handWashRequired: 손세탁 필요 여부를 true/false/null로 답한다.
			  - separateWashRequired: 단독 세탁 필요 여부를 true/false/null로 답한다.
			  사진만으로 확정할 수 없는 washRules 항목은 null로 답하고, 의류 손상이 우려되는 항목은 보수적으로 판단한다.
			{format}
			""";

	private final ChatModel chatModel;
	private final ExternalApiCircuitBreaker externalApiCircuitBreaker;
	private final OpenAiChatOptionsFactory openAiChatOptionsFactory;
	private final ClothesAnalysisResultValidator clothesAnalysisResultValidator;
	private final MeterRegistry meterRegistry;

	@Value("${external-api.openai.call-timeout:60s}")
	private Duration openAiCallTimeout;

	@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
	private String openAiModel;

	public ClothesAnalysisResDto analyze(MultipartFile image) {
		MimeType imageMimeType = validateAndResolveImage(image);

		BeanOutputConverter<ClothesAnalysisAiResDto> outputConverter =
				new BeanOutputConverter<>(ClothesAnalysisAiResDto.class);

		String promptText = ANALYSIS_PROMPT.formatted(outputConverter.getFormat());
		UserMessage userMessage = UserMessage.builder()
				.text(promptText)
				.media(new Media(imageMimeType, image.getResource()))
				.build();

		Timer.Sample openAiTimer = Timer.start(meterRegistry);
		try {
			String responseText = externalApiCircuitBreaker.execute(OPENAI_CIRCUIT, openAiCallTimeout, () ->
					chatModel.call(new Prompt(userMessage, openAiChatOptionsFactory.create()))
							.getResult()
							.getOutput()
							.getText()
			);

			ClothesAnalysisAiResDto result = outputConverter.convert(
					ClothesAnalysisResponseNormalizer.normalize(responseText)
			);
			clothesAnalysisResultValidator.validate(result);
			recordOpenAiTimer(openAiTimer, "success", null);
			return ClothesAnalysisResDto.from(result);
		} catch (ExternalApiCircuitOpenException | ExternalApiCallTimeoutException exception) {
			recordOpenAiTimer(openAiTimer, "failure", exception.getClass().getSimpleName());
			log.warn("OpenAI clothes analysis failed by circuit breaker or timeout", exception);
			throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED);
		} catch (GeneralException exception) {
			recordOpenAiTimer(openAiTimer, "failure", exception.getClass().getSimpleName());
			throw exception;
		} catch (Exception exception) {
			recordOpenAiTimer(openAiTimer, "failure", exception.getClass().getSimpleName());
			log.warn("Failed to analyze clothes image", exception);
			throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED);
		}
	}

	private void recordOpenAiTimer(Timer.Sample sample, String outcome, String exception) {
		sample.stop(Timer.builder("clothes.analysis.openai")
				.description("OpenAI latency for clothes image analysis")
				.tag("model", openAiModel)
				.tag("outcome", outcome)
				.tag("exception", exceptionBucket(exception))
				.register(meterRegistry));
	}

	private String exceptionBucket(String exception) {
		if (exception == null || exception.isBlank()) {
			return "none";
		}
		if (EXPECTED_OPENAI_EXCEPTIONS.contains(exception)) {
			return "general";
		}
		return "unexpected";
	}

	private MimeType validateAndResolveImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new GeneralException(ClothesErrorCode.IMAGE_REQUIRED);
		}

		try {
			return ImageFileValidator.detectSupportedMimeType(image);
		} catch (ImageFileSizeExceededException exception) {
			throw new GeneralException(GlobalErrorCode.FILE_SIZE_EXCEEDED);
		} catch (IllegalArgumentException exception) {
			throw new GeneralException(ClothesErrorCode.INVALID_IMAGE_TYPE);
		}
	}
}
