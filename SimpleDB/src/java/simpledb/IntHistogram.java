package simpledb;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {
    private final int bins[];
    private final int min;
    private final int max;
    private volatile int ntups;
    private volatile double avgSelectivity;

    /**
     * Create a new IntHistogram.
     *
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     *
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     *
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
        // some code goes here
        bins = new int[buckets];
        this.min = min;
        this.max = max;
        this.avgSelectivity=0.0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        //Store the current ntups and update along with the query
        int current_Ntups = ntups;
        int buckSteps = (int)((max-min)/bins.length);
        if (buckSteps == 0)
            buckSteps++;

        int buck = (v - min)/buckSteps;
        if (buck >= bins.length) buck = bins.length-1;
        int old=bins[buck];
        bins[buck]++;

        this.avgSelectivity+=2*old+1;
        //Sync volatile leave
        ntups=current_Ntups+1;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     *
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {

        double current_V = v;

        // Egde cases exception handlings
        if (op == Predicate.Op.GREATER_THAN_OR_EQ) {
            if (v<=min) return 1;
            if (v>max) return 0;
        }
        if (op == Predicate.Op.GREATER_THAN) {
            if (v < min) return 1;
            if (v >= max) return 0;
            if (current_V < max) current_V = current_V+.01; //return slightly less than full bucket
        }
        if (op == Predicate.Op.LESS_THAN) {
            if (v <= min) return 0;
            if (v > max) return 1;
            if (current_V > min) current_V = current_V-.01; //return slightly less than full bucket
        }
        if (op == Predicate.Op.LESS_THAN_OR_EQ) {
            if (v < min) return 0;
            if (v >= max) return 1;

            if (current_V < max) current_V = current_V+.99; //computations below return data exclusive of v, so be sure to include v's bin
        }
        if (op == Predicate.Op.EQUALS || op == Predicate.Op.LIKE) {
            if (v < min || v > max)
                return 0;
        }
        if (op == Predicate.Op.NOT_EQUALS) {
            if (v < min || v > max)
                return 1;
        }

        //Init bucksteps
        int buckSteps = (int)((max-min)/bins.length);
        if (buckSteps == 0)
            buckSteps++;

        if (current_V < min) current_V = min - buckSteps;
        if (current_V > max) current_V = max + buckSteps;
        int buck = (int)((current_V-min) / buckSteps);
        if (buck < 0) buck = 0;
        if (buck >= bins.length) buck = bins.length -1;
        double buckMin = (buck * buckSteps) + min;
        double buckMax = ((buck+1) * buckSteps) + min;

        int current_Ntups = ntups;
        //Examines operations and update each case scenerio
        switch (op) {
            case NOT_EQUALS: case EQUALS: case LIKE:
                double frac = ((double)(bins[buck])/(double)ntups)/(double)buckSteps;
                if (frac > 0 && frac < 1.0/(double)ntups)
                    frac = 1.0 / (double)ntups;
                if (op == Predicate.Op.EQUALS || op == Predicate.Op.LIKE)
                    return frac;
                else
                    return 1-frac;
            case GREATER_THAN_OR_EQ:
            case GREATER_THAN:
                double buckFrac = (double)(buckMax - (current_V)) / (double)(buckMax - buckMin);
                if (buckFrac > 1) buckFrac = 1.0;
                if (buckFrac < 0) buckFrac = 0;
                double buckSel =  ((double)bins[buck]/(double)ntups) * buckFrac;
                for (int i = buck+1; i < bins.length; i++) {
                    buckSel +=  ((double)bins[i]/ntups);
                }
                return buckSel;
            case LESS_THAN:
            case LESS_THAN_OR_EQ:

                buckFrac = 1.0 - ((double)(buckMax - current_V) / (double)(buckMax - buckMin));
                if (buckFrac < 0) buckFrac = 0;
                if (buckFrac > 1) buckFrac = 1.0;
                buckSel =  ((double)bins[buck]/ntups) * buckFrac;
                for (int i = buck-1; i >= 0; i--) {
                    buckSel +=  ((double)bins[i]/ntups);
                }
                return buckSel;

        }
        //update volatile
        ntups=current_Ntups;
        return 1.0;
    }

    /**
     * @return
     *     the average selectivity of this histogram.
     *
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
        return this.avgSelectivity/this.ntups/this.ntups;

    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        //Sync Volatile ntups
        int current_Ntups = ntups;
        int buckSteps = (int)((max-min)/bins.length);
        if (buckSteps == 0)
            buckSteps++;

        int start = min;
        String s = "";
        for (int i = 0; i < bins.length; i++) {
            s += "BIN " + i + " START " + start + " END " + (start + buckSteps) + " HEIGHT " + bins[i] + "\n";
            start += buckSteps;
        }
        return s;
    }
}