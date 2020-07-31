package com.zwh.spline.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class DPUtils {
  private static float sPixelDensity = -1;
  public static float dpToPixel(Context context, float dp) {
    if (sPixelDensity < 0) {
      if (context != null) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        sPixelDensity = metrics.density;
      }
    }
    return sPixelDensity * dp;
  }

  public static int dpToPixel(Context context, int dp) {
    return (int) (dpToPixel(context, (float) dp) + .5f);
  }

  public static int dpFloatToPixel(Context context, float dp) {
    return (int) (dpToPixel(context, dp) + .5f);
  }
}