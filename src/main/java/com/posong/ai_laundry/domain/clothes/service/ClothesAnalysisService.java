package com.posong.ai_laundry.domain.clothes.service;

import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisAiResDto;
import com.posong.ai_laundry.domain.clothes.dto.ClothesAnalysisResDto;
import com.posong.ai_laundry.domain.clothes.exception.ClothesErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClothesAnalysisService {

	private static final String ANALYSIS_PROMPT = """
			너는 세탁 가이드를 만드는 의류 분석가다.
			입력된 옷 사진을 보고 아래 기준으로만 판단해라.

			- categoryName: 상의, 하의, 아우터, 원피스, 치마, 속옷, 잠옷, 기타 중 하나로 답한다.
			- name: 사용자가 구분하기 쉬운 짧은 옷 이름으로 답한다.
			- material: 사진만 보고 추정 가능한 소재를 자연스럽게 적는다. 혼용률은 추정치임을 반영해도 된다.
			- color: 대표 색상을 한글로 답한다.
			- washingMethod: 세탁 온도, 권장 코스, 세제 종류, 단독 세탁 여부, 손세탁 필요 여부를 포함해서 2~3문장으로 자세히 설명한다.
			  단순히 결론만 짧게 쓰지 말고, 왜 그런 세탁 방법이 적절한지도 짧게 덧붙인다.
			  예를 들어 중성세제가 필요하면 중성세제를 쓰는 이유를 한 문장 안에 자연스럽게 포함한다.
			- caution: 수축, 이염, 건조기, 표백제, 다림질 등 주의사항을 2~3문장으로 자세히 설명한다.
			  단순 경고만 적지 말고, 어떤 문제가 생길 수 있는지와 피하는 방법을 함께 적는다.

			추정이 필요한 항목은 가장 보수적으로 판단하고, 모르면 과장하지 마라.
			사용자가 바로 세탁에 참고할 수 있도록 자연스럽고 구체적인 한국어 문장으로 작성해라.
			반드시 JSON으로만 응답해라.
			{format}
			""";

	private final ChatModel chatModel;

	@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
	private String openAiModel;

	public ClothesAnalysisResDto analyze(MultipartFile image) {
		validateImage(image);

		BeanOutputConverter<ClothesAnalysisAiResDto> outputConverter =
				new BeanOutputConverter<>(ClothesAnalysisAiResDto.class);

		String promptText = ANALYSIS_PROMPT.formatted(outputConverter.getFormat());
		UserMessage userMessage = UserMessage.builder()
				.text(promptText)
				.media(new Media(resolveMimeType(image), image.getResource()))
				.build();

		try {
			String responseText = chatModel.call(
							new Prompt(userMessage, OpenAiChatOptions.builder().model(openAiModel).build()))
					.getResult()
					.getOutput()
					.getText();

			ClothesAnalysisAiResDto result = outputConverter.convert(responseText);
			validateResult(result);
			return ClothesAnalysisResDto.from(result);
		} catch (Exception exception) {
			throw new GeneralException(ClothesErrorCode.CLOTHES_ANALYSIS_FAILED);
		}
	}

	private void validateImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new GeneralException(ClothesErrorCode.IMAGE_REQUIRED);
		}

		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new GeneralException(ClothesErrorCode.INVALID_IMAGE_TYPE);
		}
	}

	private MimeType resolveMimeType(MultipartFile image) {
		String contentType = image.getContentType();
		return contentType == null ? MimeTypeUtils.IMAGE_JPEG : MimeType.valueOf(contentType);
	}

	private void validateResult(ClothesAnalysisAiResDto result) {
		if (result == null
				|| isBlank(result.categoryName())
				|| isBlank(result.name())
				|| isBlank(result.material())
				|| isBlank(result.color())
				|| isBlank(result.washingMethod())
				|| isBlank(result.caution())) {
			throw new GeneralException(ClothesErrorCode.INVALID_ANALYSIS_RESULT);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
