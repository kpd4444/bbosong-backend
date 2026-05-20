package com.posong.ai_laundry.domain.weather.service;

import com.posong.ai_laundry.domain.weather.dto.LaundryRecommendationResDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherRecommendationService {

	private static final int HIGH_RAIN_PROBABILITY = 60;
	private static final int MODERATE_RAIN_PROBABILITY = 40;
	private static final int VERY_HUMID = 85;
	private static final int HUMID = 75;
	private static final int GOOD_HUMIDITY = 65;
	private static final int COLD_TEMPERATURE = 5;

	public List<LaundryRecommendationResDto> recommend(WeatherForecastSnapshot snapshot) {
		List<LaundryRecommendationResDto> recommendations = new ArrayList<>();

		if (hasPrecipitation(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"실내건조 추천",
					"비나 눈 예보가 있어요. 오늘은 바깥보다 실내에서 말리는 편이 안전해요.",
					"INDOOR"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"두꺼운 빨래는 미루기",
					"수건, 이불처럼 마르는 데 오래 걸리는 빨래는 날씨가 나아진 뒤가 좋아요.",
					"DELAY"
			));
			return recommendations;
		}

		if (hasHighRainChance(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"실내건조 추천",
					"강수확률이 높아요. 갑자기 비가 올 수 있으니 실내건조를 추천해요.",
					"INDOOR"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"외출 전 확인",
					"밖에 널 계획이라면 나가기 전에 하늘 상태를 한 번 더 확인해 주세요.",
					"DELAY"
			));
			return recommendations;
		}

		if (isVeryHumid(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"제습 건조 추천",
					"습도가 높아 빨래가 눅눅하게 마를 수 있어요. 제습기나 환기를 함께 사용해 주세요.",
					"DEHUMIDIFY"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"세탁량 줄이기",
					"오늘 꼭 해야 한다면 얇은 옷 위주로 적게 세탁하는 편이 좋아요.",
					"LAUNDRY"
			));
			return recommendations;
		}

		if (isCold(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"실내건조 추천",
					"기온이 낮아 바깥에서는 빨래가 더디게 마를 수 있어요.",
					"INDOOR"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"건조 시간 여유두기",
					"두꺼운 옷은 평소보다 건조 시간이 오래 걸릴 수 있어요.",
					"DELAY"
			));
			return recommendations;
		}

		if (isGoodForOutdoorDry(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"실외건조 추천",
					"비 예보가 낮고 습도도 높지 않아요. 바깥 건조를 하기 좋은 날이에요.",
					"SUN"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"두꺼운 빨래 가능",
					"수건이나 맨투맨처럼 두꺼운 빨래도 오늘은 시도해 볼 만해요.",
					"LAUNDRY"
			));
			return recommendations;
		}

		if (isHumid(snapshot) || hasModerateRainChance(snapshot)) {
			recommendations.add(new LaundryRecommendationResDto(
					"가벼운 세탁 추천",
					"날씨가 애매해서 얇은 옷이나 속옷처럼 잘 마르는 빨래를 추천해요.",
					"LAUNDRY"
			));
			recommendations.add(new LaundryRecommendationResDto(
					"실내 통풍 필요",
					"실내에서 말릴 때는 창문을 열거나 선풍기를 함께 사용하면 좋아요.",
					"INDOOR"
			));
			return recommendations;
		}

		recommendations.add(new LaundryRecommendationResDto(
				"일반 세탁 추천",
				"세탁하기 무난한 날씨예요. 평소처럼 세탁해도 괜찮아요.",
				"LAUNDRY"
		));
		recommendations.add(new LaundryRecommendationResDto(
				"건조 상태 확인",
				"건조가 끝난 뒤 두꺼운 부분은 덜 말랐는지 한 번 확인해 주세요.",
				"INDOOR"
		));
		return recommendations;
	}

	private boolean hasPrecipitation(WeatherForecastSnapshot snapshot) {
		return !"없음".equals(snapshot.precipitationType())
				&& !"알 수 없음".equals(snapshot.precipitationType());
	}

	private boolean hasHighRainChance(WeatherForecastSnapshot snapshot) {
		return isGreaterThanOrEqual(snapshot.rainProbability(), HIGH_RAIN_PROBABILITY);
	}

	private boolean hasModerateRainChance(WeatherForecastSnapshot snapshot) {
		return isGreaterThanOrEqual(snapshot.rainProbability(), MODERATE_RAIN_PROBABILITY);
	}

	private boolean isVeryHumid(WeatherForecastSnapshot snapshot) {
		return isGreaterThanOrEqual(snapshot.humidity(), VERY_HUMID);
	}

	private boolean isHumid(WeatherForecastSnapshot snapshot) {
		return isGreaterThanOrEqual(snapshot.humidity(), HUMID);
	}

	private boolean isCold(WeatherForecastSnapshot snapshot) {
		return isLessThanOrEqual(snapshot.temperature(), COLD_TEMPERATURE);
	}

	private boolean isGoodForOutdoorDry(WeatherForecastSnapshot snapshot) {
		return "맑음".equals(snapshot.skyStatus())
				&& "없음".equals(snapshot.precipitationType())
				&& isLessThan(snapshot.rainProbability(), MODERATE_RAIN_PROBABILITY)
				&& isLessThan(snapshot.humidity(), GOOD_HUMIDITY)
				&& isGreaterThan(snapshot.temperature(), COLD_TEMPERATURE);
	}

	private boolean isGreaterThanOrEqual(Integer value, int threshold) {
		return value != null && value >= threshold;
	}

	private boolean isGreaterThan(Integer value, int threshold) {
		return value != null && value > threshold;
	}

	private boolean isLessThan(Integer value, int threshold) {
		return value != null && value < threshold;
	}

	private boolean isLessThanOrEqual(Integer value, int threshold) {
		return value != null && value <= threshold;
	}
}
