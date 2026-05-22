package com.posong.ai_laundry.domain.weather.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class KmaGridConverter {

	private static final double EARTH_RADIUS_KM = 6371.00877;
	private static final double GRID_INTERVAL_KM = 5.0;
	private static final double STANDARD_LATITUDE_1 = 30.0;
	private static final double STANDARD_LATITUDE_2 = 60.0;
	private static final double REFERENCE_LONGITUDE = 126.0;
	private static final double REFERENCE_LATITUDE = 38.0;
	private static final double REFERENCE_X = 43.0;
	private static final double REFERENCE_Y = 136.0;
	private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

	public KmaGridCoordinate convert(BigDecimal latitude, BigDecimal longitude) {
		return convert(latitude.doubleValue(), longitude.doubleValue());
	}

	public KmaGridCoordinate convert(double latitude, double longitude) {
		double re = EARTH_RADIUS_KM / GRID_INTERVAL_KM;
		double slat1 = STANDARD_LATITUDE_1 * DEGREES_TO_RADIANS;
		double slat2 = STANDARD_LATITUDE_2 * DEGREES_TO_RADIANS;
		double olon = REFERENCE_LONGITUDE * DEGREES_TO_RADIANS;
		double olat = REFERENCE_LATITUDE * DEGREES_TO_RADIANS;

		double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
				/ Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

		double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

		double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
		ro = re * sf / Math.pow(ro, sn);

		double ra = Math.tan(Math.PI * 0.25 + latitude * DEGREES_TO_RADIANS * 0.5);
		ra = re * sf / Math.pow(ra, sn);

		double theta = longitude * DEGREES_TO_RADIANS - olon;
		if (theta > Math.PI) {
			theta -= 2.0 * Math.PI;
		}
		if (theta < -Math.PI) {
			theta += 2.0 * Math.PI;
		}
		theta *= sn;

		int nx = (int) Math.floor(ra * Math.sin(theta) + REFERENCE_X + 0.5);
		int ny = (int) Math.floor(ro - ra * Math.cos(theta) + REFERENCE_Y + 0.5);
		return new KmaGridCoordinate(nx, ny);
	}
}
