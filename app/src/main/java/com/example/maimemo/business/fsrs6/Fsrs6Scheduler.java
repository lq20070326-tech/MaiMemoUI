package com.example.maimemo.business.fsrs6;

/**
 * FSRS-6 scheduler using the official 21 default parameters and DSR equations.
 * Rating scale follows FSRS: 1=Again, 2=Hard, 3=Good, 4=Easy.
 * This class implements scheduling/inference only; parameter optimization is not included.
 */
public final class Fsrs6Scheduler {
    public static final int AGAIN = 1, HARD = 2, GOOD = 3, EASY = 4;
    public static final double DEFAULT_RETENTION = 0.90;
    public static final double[] DEFAULT_WEIGHTS = {
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194,
            0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629,
            1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
    };

    public static final class Result {
        public final double stability, difficulty, intervalDays, retrievability;
        public final int lapses, rating;
        public Result(double stability, double difficulty, double intervalDays,
                      double retrievability, int lapses, int rating) {
            this.stability = stability; this.difficulty = difficulty;
            this.intervalDays = intervalDays; this.retrievability = retrievability;
            this.lapses = lapses; this.rating = rating;
        }
    }

    private Fsrs6Scheduler() { }

    public static Result review(double stability, double difficulty, int previousReviews,
                                int previousLapses, int rating, double elapsedDays) {
        return review(stability, difficulty, previousReviews, previousLapses, rating,
                elapsedDays, DEFAULT_RETENTION, DEFAULT_WEIGHTS);
    }

    public static Result review(double stability, double difficulty, int previousReviews,
                                int previousLapses, int rating, double elapsedDays,
                                double desiredRetention, double[] weights) {
        validateRating(rating);
        if (weights == null || weights.length != 21) throw new IllegalArgumentException("FSRS-6 requires exactly 21 weights");
        double[] w = weights;
        double t = Math.max(0.0, elapsedDays);
        double s, d, r;
        int lapses = previousLapses;
        if (previousReviews <= 0 || stability <= 0.0 || difficulty <= 0.0) {
            s = Math.max(0.1, w[rating - 1]);
            d = clamp(w[4] - Math.exp(w[5] * (rating - 1.0)) + 1.0, 1.0, 10.0);
            if (rating == AGAIN) lapses++;
            r = 0.0;
        } else {
            s = clamp(stability, 0.1, 36500.0);
            d = clamp(difficulty, 1.0, 10.0);
            r = retrievability(s, t, w);
            if (rating == AGAIN) lapses++;
            double nextD = d - w[6] * (rating - 3.0) * (10.0 - d) / 9.0;
            double easyD0 = w[4] - Math.exp(w[5] * (EASY - 1.0)) + 1.0;
            d = clamp(w[7] * easyD0 + (1.0 - w[7]) * nextD, 1.0, 10.0);

            if (t <= 0.0) {
                // FSRS-6 same-day stability update.
                s = s * Math.exp(w[17] * (rating - 3.0 + w[18])) * Math.pow(s, -w[19]);
            } else if (rating == AGAIN) {
                // Stability after forgetting.
                s = w[11] * Math.pow(d, -w[12]) * (Math.pow(s + 1.0, w[13]) - 1.0)
                        * Math.exp(w[14] * (1.0 - r));
            } else {
                // Stability after successful recall; w[15]/w[16] adjust Hard/Easy.
                double gradeFactor = rating == HARD ? w[15] : rating == EASY ? w[16] : 1.0;
                double increase = Math.exp(w[8]) * (11.0 - d) * Math.pow(s, -w[9])
                        * (Math.exp(w[10] * (1.0 - r)) - 1.0) * gradeFactor;
                s = s * (1.0 + increase);
            }
        }
        s = clamp(finiteOr(s, 0.1), 0.1, 36500.0);
        d = clamp(finiteOr(d, 5.0), 1.0, 10.0);
        double retention = clamp(desiredRetention, 0.70, 0.99);
        double decay = w[20];
        double factor = Math.pow(0.9, -1.0 / decay) - 1.0;
        double interval = s / factor * (Math.pow(retention, -1.0 / decay) - 1.0);
        // FSRS intervals are whole-day intervals at the scheduler boundary (minimum 1 day).
        interval = clamp(Math.round(finiteOr(interval, 1.0)), 1.0, 36500.0);
        return new Result(s, d, interval, r, lapses, rating);
    }

    public static double retrievability(double stabilityDays, double elapsedDays) {
        return retrievability(stabilityDays, elapsedDays, DEFAULT_WEIGHTS);
    }

    public static double retrievability(double stabilityDays, double elapsedDays, double[] weights) {
        if (stabilityDays <= 0) return 0.0;
        double decay = weights != null && weights.length == 21 ? weights[20] : DEFAULT_WEIGHTS[20];
        double factor = Math.pow(0.9, -1.0 / decay) - 1.0;
        return clamp(Math.pow(1.0 + factor * Math.max(0.0, elapsedDays) / stabilityDays, -decay), 0.0, 1.0);
    }

    private static void validateRating(int rating) {
        if (rating < AGAIN || rating > EASY) throw new IllegalArgumentException("rating must be 1..4");
    }
    private static double finiteOr(double value, double fallback) { return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value; }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
