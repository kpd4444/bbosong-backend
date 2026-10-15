package com.posong.ai_laundry.global.file;

import com.posong.ai_laundry.global.error.code.GlobalErrorCode;
import com.posong.ai_laundry.global.error.exception.GeneralException;

public class ImageDimensionsExceededException extends GeneralException {
	public ImageDimensionsExceededException() {
		super(GlobalErrorCode.IMAGE_DIMENSIONS_EXCEEDED);
	}
}
