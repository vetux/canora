package com.phaseshifter.canora.utils.android.mediastyle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.phaseshifter.canora.utils.android.palette.Palette;

import java.util.List;

import static androidx.core.graphics.ColorUtils.calculateLuminance;

/**
 * Extraction of the relevant code for the MediaStyle notification colors and gradient artwork
 * from com.android.systemui.statusbar.notification.MediaNotificationProcessor
 *
 * @see "https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/statusbar/notification/MediaNotificationProcessor.java"
 */
public class MediaStyleUtils {
    /**
     * The fraction below which we select the vibrant instead of the light/dark vibrant color
     */
    private static final float POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f;
    /**
     * Minimum saturation that a muted color must have if there exists if deciding between two
     * colors
     */
    private static final float MIN_SATURATION_WHEN_DECIDING = 0.19f;
    /**
     * Minimum fraction that any color must have to be picked up as a text color
     */
    private static final double MINIMUM_IMAGE_FRACTION = 0.002;
    /**
     * The population fraction to select the dominant color as the text color over a the colored
     * ones.
     */
    private static final float POPULATION_FRACTION_FOR_DOMINANT = 0.01f;
    /**
     * The population fraction to select a white or black color as the background over a color.
     */
    private static final float POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f;
    private static final float BLACK_MAX_LIGHTNESS = 0.08f;
    private static final float WHITE_MIN_LIGHTNESS = 0.90f;
    private static final int RESIZE_BITMAP_AREA = 150 * 150;

    private static final Palette.Filter mBlackWhiteFilter = (rgb, hsl) -> !isWhiteOrBlack(hsl);

    public static int getForegroundColor(Drawable drawable) {
        if (drawable == null)
            throw new IllegalArgumentException("Image null");

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        int area = width * height;
        if (area > RESIZE_BITMAP_AREA) {
            double factor = Math.sqrt((float) RESIZE_BITMAP_AREA / area);
            width = (int) (factor * width);
            height = (int) (factor * height);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        return getForegroundColor(bitmap);
    }

    public static int getForegroundColor(Bitmap bitmap) {
        Palette.Builder paletteBuilder = generateArtworkPaletteBuilder(bitmap);
        Palette palette = paletteBuilder.generate();
        Palette.Swatch backgroundSwatch = findBackgroundSwatch(palette);

        int backgroundColor = backgroundSwatch.getRgb();
        // we want most of the full region again, slightly shifted to the right
        float textColorStartWidthFraction = 0.4f;
        paletteBuilder.setRegion((int) (bitmap.getWidth() * textColorStartWidthFraction), 0,
                bitmap.getWidth(),
                bitmap.getHeight());
        // We're not filtering on white or black
        if (!isWhiteOrBlack(backgroundSwatch.getHsl())) {
            final float backgroundHue = backgroundSwatch.getHsl()[0];
            paletteBuilder.addFilter((rgb, hsl) -> {
                // at least 10 degrees hue difference
                float diff = Math.abs(hsl[0] - backgroundHue);
                return diff > 10 && diff < 350;
            });
        }

        paletteBuilder.addFilter(mBlackWhiteFilter);
        palette = paletteBuilder.generate();
        return selectForegroundColor(backgroundColor, palette);
    }

    public static Bitmap getGradientArtwork(Bitmap original) {
        throw new UnsupportedOperationException("Not Implemented.");
    }

    private static int selectForegroundColor(int backgroundColor, Palette palette) {
        if (isColorLight(backgroundColor)) {
            return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getDarkMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.BLACK);
        } else {
            return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(),
                    palette.getVibrantSwatch(),
                    palette.getLightMutedSwatch(),
                    palette.getMutedSwatch(),
                    palette.getDominantSwatch(),
                    Color.WHITE);
        }
    }

    private static int selectForegroundColorForSwatches(Palette.Swatch moreVibrant,
                                                        Palette.Swatch vibrant, Palette.Swatch moreMutedSwatch, Palette.Swatch mutedSwatch,
                                                        Palette.Swatch dominantSwatch, int fallbackColor) {
        Palette.Swatch coloredCandidate = selectVibrantCandidate(moreVibrant, vibrant);
        if (coloredCandidate == null) {
            coloredCandidate = selectMutedCandidate(mutedSwatch, moreMutedSwatch);
        }
        if (coloredCandidate != null) {
            if (dominantSwatch == coloredCandidate) {
                return coloredCandidate.getRgb();
            } else if ((float) coloredCandidate.getPopulation() / dominantSwatch.getPopulation()
                    < POPULATION_FRACTION_FOR_DOMINANT
                    && dominantSwatch.getHsl()[1] > MIN_SATURATION_WHEN_DECIDING) {
                return dominantSwatch.getRgb();
            } else {
                return coloredCandidate.getRgb();
            }
        } else if (hasEnoughPopulation(dominantSwatch)) {
            return dominantSwatch.getRgb();
        } else {
            return fallbackColor;
        }
    }

    private static Palette.Swatch selectMutedCandidate(Palette.Swatch first,
                                                       Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            float firstSaturation = first.getHsl()[1];
            float secondSaturation = second.getHsl()[1];
            float populationFraction = first.getPopulation() / (float) second.getPopulation();
            if (firstSaturation * populationFraction > secondSaturation) {
                return first;
            } else {
                return second;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private static boolean hasEnoughPopulation(Palette.Swatch swatch) {
        // We want a fraction that is at least 1% of the image
        return swatch != null
                && (swatch.getPopulation() / (float) RESIZE_BITMAP_AREA > MINIMUM_IMAGE_FRACTION);
    }

    private static Palette.Swatch selectVibrantCandidate(Palette.Swatch first, Palette.Swatch second) {
        boolean firstValid = hasEnoughPopulation(first);
        boolean secondValid = hasEnoughPopulation(second);
        if (firstValid && secondValid) {
            int firstPopulation = first.getPopulation();
            int secondPopulation = second.getPopulation();
            if (firstPopulation / (float) secondPopulation
                    < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
                return second;
            } else {
                return first;
            }
        } else if (firstValid) {
            return first;
        } else if (secondValid) {
            return second;
        }
        return null;
    }

    private static boolean isColorLight(int backgroundColor) {
        return calculateLuminance(backgroundColor) > 0.5f;
    }

    private static boolean isWhiteOrBlack(float[] hsl) {
        return isBlack(hsl) || isWhite(hsl);
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private static boolean isBlack(float[] hslColor) {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS;
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private static boolean isWhite(float[] hslColor) {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS;
    }

    /**
     * Finds an appropriate background swatch from the palette of media artwork.
     *
     * @param palette Artwork palette
     * @return Swatch that should be used as the background of the media notification.
     */
    private static Palette.Swatch findBackgroundSwatch(Palette palette) {
        // by default we use the dominant palette
        Palette.Swatch dominantSwatch = palette.getDominantSwatch();
        if (dominantSwatch == null) {
            return new Palette.Swatch(Color.WHITE, 100);
        }
        if (!isWhiteOrBlack(dominantSwatch.getHsl())) {
            return dominantSwatch;
        }
        // Oh well, we selected black or white. Lets look at the second color!
        List<Palette.Swatch> swatches = palette.getSwatches();
        float highestNonWhitePopulation = -1;
        Palette.Swatch second = null;
        for (Palette.Swatch swatch : swatches) {
            if (swatch != dominantSwatch
                    && swatch.getPopulation() > highestNonWhitePopulation
                    && !isWhiteOrBlack(swatch.getHsl())) {
                second = swatch;
                highestNonWhitePopulation = swatch.getPopulation();
            }
        }
        if (second == null) {
            return dominantSwatch;
        }
        if (dominantSwatch.getPopulation() / highestNonWhitePopulation
                > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
            // The dominant swatch is very dominant, lets take it!
            // We're not filtering on white or black
            return dominantSwatch;
        } else {
            return second;
        }
    }

    /**
     * Generate a palette builder for media artwork.
     * <p>
     * For producing a smooth background transition, the palette is extracted from only the left
     * side of the artwork.
     *
     * @param artwork Media artwork
     * @return Builder that generates the {@link Palette} for the media artwork.
     */
    private static Palette.Builder generateArtworkPaletteBuilder(Bitmap artwork) {
        // for the background we only take the left side of the image to ensure
        // a smooth transition
        return Palette.from(artwork)
                .setRegion(0, 0, artwork.getWidth() / 2, artwork.getHeight())
                .clearFilters() // we want all colors, red / white / black ones too!
                .resizeBitmapArea(RESIZE_BITMAP_AREA);
    }
}
