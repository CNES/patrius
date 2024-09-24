/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit test class to evaluate the following class :
 * <ul>
 * <li>{@link PiecewiseFunction}</li>
 * <li>{@link IntervalsFunction}</li>
 * </ul>
 *
 * @author bonitt
 */
public class IntervalsFunctionsTest {

    /** Function duration. */
    private double duration;

    /** Initial state */
    private SpacecraftState state;

    /** Piecewise function. */
    private PiecewiseFunction piecewiseFct;

    /** Dates interval. */
    private List<AbsoluteDateInterval> intervals;

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link PiecewiseFunction} values computation feature.
     * 
     * @testedMethod {@link PiecewiseFunction#value(SpacecraftState)}
     * 
     * @passCriteria The values are computed as expected.
     */
    @Test
    public void testPiecewiseFunctionValues() throws PatriusException {

        // Evaluate the values
        Assert.assertEquals(0., this.piecewiseFct.value(this.state.shiftedBy(-1e12)), 0.);
        Assert.assertEquals(0., this.piecewiseFct.value(this.state), 0.);
        Assert.assertEquals(0., this.piecewiseFct.value(this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(1., this.piecewiseFct.value(this.state.shiftedBy(this.duration)), 0.);
        Assert.assertEquals(2., this.piecewiseFct.value(this.state.shiftedBy(2 * this.duration)), 0.);
        Assert.assertEquals(3., this.piecewiseFct.value(this.state.shiftedBy(3 * this.duration)), 0.);
        Assert.assertEquals(3., this.piecewiseFct.value(this.state.shiftedBy(1e12)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link PiecewiseFunction} derivatives computation feature.
     * 
     * @testedMethod {@link PiecewiseFunction#derivativeValue(Parameter, SpacecraftState)}
     * 
     * @passCriteria The derivatives are computed as expected.
     */
    @Test
    public void testPiecewiseFunctionDerivatives() throws PatriusException {

        final int nbPieces = 4;
        final List<Parameter> params = this.piecewiseFct.getParameters();

        // Evaluate the derivatives
        Assert.assertEquals(1.,
                this.piecewiseFct.derivativeValue(params.get(0), this.state.shiftedBy(-1e12)), 0.);
        Assert.assertEquals(0.,
                this.piecewiseFct.derivativeValue(params.get(1), this.state.shiftedBy(-1e12)), 0.);

        Assert.assertEquals(0.,
                this.piecewiseFct.derivativeValue(params.get(0), this.state.shiftedBy(this.duration)),
                0.);
        Assert.assertEquals(1.,
                this.piecewiseFct.derivativeValue(params.get(1), this.state.shiftedBy(this.duration)),
                0.);
        Assert.assertEquals(
                0.,
                this.piecewiseFct.derivativeValue(params.get(0),
                        this.state.shiftedBy(this.duration + 10.)), 0.);
        Assert.assertEquals(
                1.,
                this.piecewiseFct.derivativeValue(params.get(1),
                        this.state.shiftedBy(this.duration + 10.)), 0.);

        Assert.assertEquals(0., this.piecewiseFct.derivativeValue(params.get(0),
                this.state.shiftedBy(2 * this.duration)), 0.);
        Assert.assertEquals(1., this.piecewiseFct.derivativeValue(params.get(2),
                this.state.shiftedBy(2 * this.duration)), 0.);

        Assert.assertEquals(0.,
                this.piecewiseFct.derivativeValue(params.get(0), this.state.shiftedBy(1e12)), 0.);
        Assert.assertEquals(
                1.,
                this.piecewiseFct.derivativeValue(params.get(nbPieces - 1),
                        this.state.shiftedBy(1e12)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link PiecewiseFunction} error cases.
     * 
     * @testedMethod {@link PiecewiseFunction#PiecewiseFunction(List, List)}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testPiecewiseFunctionExceptions() throws PatriusException {

        List<AbsoluteDate> dates = new ArrayList<>();
        final List<IParamDiffFunction> cstFcts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cstFcts.add(new ConstantFunction(+1.0 * i));
        }

        // When dates.size() != cstFuncs.size() - 1
        try {
            new PiecewiseFunction(cstFcts, dates);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the dates list isn't ordered chronologically
        dates.add(this.state.getDate());
        dates.add(this.state.getDate().shiftedBy(-10.));
        dates.add(this.state.getDate().shiftedBy(-20.));
        try {
            new PiecewiseFunction(cstFcts, dates);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the dates list contains {@link AbsoluteDate#PAST_INFINITY}
        dates = new ArrayList<>();
        dates.add(AbsoluteDate.PAST_INFINITY);
        dates.add(this.state.getDate());
        try {
            new PiecewiseFunction(cstFcts, dates);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the dates list contains {@link AbsoluteDate#FUTURE_INFINITY}
        dates = new ArrayList<>();
        dates.add(this.state.getDate());
        dates.add(AbsoluteDate.FUTURE_INFINITY);
        try {
            new PiecewiseFunction(cstFcts, dates);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the dates list contains the same date twice
        dates = new ArrayList<>();
        dates.add(this.state.getDate());
        dates.add(this.state.getDate());
        dates.add(this.state.getDate().shiftedBy(10.));

        try {
            new PiecewiseFunction(cstFcts, dates);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link IntervalsFunction} values computation feature.
     * 
     * @testedMethod {@link IntervalsFunction#value(SpacecraftState)}
     * 
     * @passCriteria The values are computed as expected.
     */
    @Test
    public void testIntervalsFunctionValues() throws PatriusException {

        // Interval function initialization
        final List<IParamDiffFunction> cstFcts = new ArrayList<>();
        final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions = new TreeMap<>();

        final Parameter[] params = new Parameter[this.intervals.size()];
        for (int i = 0; i < this.intervals.size(); i++) {
            params[i] = new Parameter("cf " + i, i + 0.5);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
            mapOfFunctions.put(this.intervals.get(i), cstFunc);
        }
        final IntervalsFunction fct = new IntervalsFunction(cstFcts, this.intervals);
        final IntervalsFunction fctBis = new IntervalsFunction(mapOfFunctions);

        // ------- Check value computation -------

        Assert.assertEquals(0.5, fct.value(this.state.shiftedBy(.1)), 0.);
        Assert.assertEquals(0.5, fct.value(this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(1.5, fct.value(this.state.shiftedBy(2 * this.duration)), 0.);
        Assert.assertEquals(2.5, fct.value(this.state.shiftedBy(3 * this.duration)), 0.);
        Assert.assertEquals(2.5, fct.value(this.state.shiftedBy(4 * this.duration)), 0.);
        Assert.assertEquals(3.5, fct.value(this.state.shiftedBy((4 * this.duration) + 0.1)), 0.);
        Assert.assertEquals(3.5, fct.value(this.state.shiftedBy(5 * this.duration)), 0.);

        Assert.assertEquals(0.5, fctBis.value(this.state.shiftedBy(.1)), 0.);
        Assert.assertEquals(0.5, fctBis.value(this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(1.5, fctBis.value(this.state.shiftedBy(2 * this.duration)), 0.);
        Assert.assertEquals(2.5, fctBis.value(this.state.shiftedBy(3 * this.duration)), 0.);
        Assert.assertEquals(2.5, fctBis.value(this.state.shiftedBy(4 * this.duration)), 0.);
        Assert.assertEquals(3.5, fctBis.value(this.state.shiftedBy((4 * this.duration) + 0.1)), 0.);
        Assert.assertEquals(3.5, fctBis.value(this.state.shiftedBy(5 * this.duration)), 0.);
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link IntervalsFunction} derivatives computation feature.
     * 
     * @testedMethod {@link IntervalsFunction#derivativeValue(Parameter, SpacecraftState)}
     * 
     * @passCriteria The derivatives are computed as expected.
     */
    @Test
    public void testIntervalsFunctionDerivatives() throws PatriusException {

        // Interval function initialization
        final List<IParamDiffFunction> cstFcts = new ArrayList<>();
        final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions = new TreeMap<>();

        final Parameter[] params = new Parameter[this.intervals.size()];
        for (int i = 0; i < this.intervals.size(); i++) {
            params[i] = new Parameter("cf " + i, i + 0.5);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
            mapOfFunctions.put(this.intervals.get(i), cstFunc);
        }
        final IntervalsFunction fct = new IntervalsFunction(cstFcts, this.intervals);
        final IntervalsFunction fctBis = new IntervalsFunction(mapOfFunctions);

        // ------- Check derivatives computation -------
        Assert.assertEquals(1., fct.derivativeValue(params[0], this.state.shiftedBy(.1)), 0.);
        Assert.assertEquals(1., fct.derivativeValue(params[0], this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(0., fct.derivativeValue(params[1], this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(0., fct.derivativeValue(params[0], this.state.shiftedBy(2 * this.duration)),
                0.);
        Assert.assertEquals(1., fct.derivativeValue(params[1], this.state.shiftedBy(2 * this.duration)),
                0.);

        Assert.assertEquals(0., fct.derivativeValue(params[0], this.state.shiftedBy(3 * this.duration)),
                0.);
        Assert.assertEquals(1., fct.derivativeValue(params[2], this.state.shiftedBy(3 * this.duration)),
                0.);

        Assert.assertEquals(0., fct.derivativeValue(params[0], this.state.shiftedBy(4 * this.duration)),
                0.);
        Assert.assertEquals(1., fct.derivativeValue(params[2], this.state.shiftedBy(4 * this.duration)),
                0.);

        Assert.assertEquals(0., fct.derivativeValue(params[0], this.state.shiftedBy(4 * this.duration)),
                0.);
        Assert.assertEquals(1., fct.derivativeValue(params[3], this.state.shiftedBy(5 * this.duration)),
                0.);

        Assert.assertEquals(1., fctBis.derivativeValue(params[0], this.state.shiftedBy(.1)), 0.);
        Assert.assertEquals(1., fctBis.derivativeValue(params[0], this.state.shiftedBy(10.)), 0.);
        Assert.assertEquals(0., fctBis.derivativeValue(params[1], this.state.shiftedBy(10.)), 0.);

        Assert.assertEquals(0.,
                fctBis.derivativeValue(params[0], this.state.shiftedBy(2 * this.duration)), 0.);
        Assert.assertEquals(1.,
                fctBis.derivativeValue(params[1], this.state.shiftedBy(2 * this.duration)), 0.);

        Assert.assertEquals(0.,
                fctBis.derivativeValue(params[0], this.state.shiftedBy(3 * this.duration)), 0.);
        Assert.assertEquals(1.,
                fctBis.derivativeValue(params[2], this.state.shiftedBy(3 * this.duration)), 0.);

        Assert.assertEquals(0.,
                fctBis.derivativeValue(params[0], this.state.shiftedBy(4 * this.duration)), 0.);
        Assert.assertEquals(1.,
                fctBis.derivativeValue(params[2], this.state.shiftedBy(4 * this.duration)), 0.);

        Assert.assertEquals(0.,
                fctBis.derivativeValue(params[0], this.state.shiftedBy(4 * this.duration)), 0.);
        Assert.assertEquals(1.,
                fctBis.derivativeValue(params[3], this.state.shiftedBy(5 * this.duration)), 0.);
    }

    /**
     * @description Evaluate the {@link IntervalsFunction} extra features.
     * 
     * @testedMethod {@link IntervalsFunction#isDifferentiableBy(Parameter)}
     * 
     * @passCriteria The function behave as expected.
     */
    @Test
    public void testIntervalsFunctionExtraFeatures() {

        // Interval function initialization
        final List<IParamDiffFunction> cstFcts = new ArrayList<>();
        final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions = new TreeMap<>();

        final Parameter[] params = new Parameter[this.intervals.size()];
        for (int i = 0; i < this.intervals.size(); i++) {
            params[i] = new Parameter("cf " + i, i + 0.5);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
            mapOfFunctions.put(this.intervals.get(i), cstFunc);
        }
        final IntervalsFunction fct = new IntervalsFunction(cstFcts, this.intervals);
        final IntervalsFunction fctBis = new IntervalsFunction(mapOfFunctions);

        // Check the isDifferentiableBy(Parameter) method behavior
        for (final IParamDiffFunction cstFct : cstFcts) {
            for (final Parameter param : cstFct.getParameters()) {
                Assert.assertTrue(fct.isDifferentiableBy(param));
                Assert.assertTrue(fctBis.isDifferentiableBy(param));
            }
        }
        Assert.assertFalse(fct.isDifferentiableBy(new Parameter("newParam", 1.)));
        Assert.assertFalse(fctBis.isDifferentiableBy(new Parameter("newParam", 1.)));
    }

    /**
     * @throws PatriusException if attitude cannot be computed if attitude events cannot be computed
     * @description Evaluate the {@link IntervalsFunction} error cases.
     * 
     * @testedMethod {@link IntervalsFunction#IntervalsFunction(List, List)}
     * @testedMethod {@link IntervalsFunction#IntervalsFunction(Map)}
     * 
     * @passCriteria The expected exceptions are returned.
     */
    @Test
    public void testIntervalsFunctionExceptions() throws PatriusException {

        // Interval function initialization
        final AbsoluteDate date = this.state.getDate();
        ArrayList<IParamDiffFunction> cstFcts = new ArrayList<>();
        final Map<AbsoluteDateInterval, IParamDiffFunction> mapOfFunctions = new TreeMap<>();

        Parameter[] params = new Parameter[this.intervals.size()];
        for (int i = 0; i < this.intervals.size(); i++) {
            params[i] = new Parameter("cf " + i, i + 0.5);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
            mapOfFunctions.put(this.intervals.get(i), cstFunc);
        }
        IntervalsFunction fct = new IntervalsFunction(cstFcts, this.intervals);

        // ------- Check the exception cases -------

        // When the intervals don't cover the given date
        try {
            fct.value(this.state.shiftedBy(-10.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            fct.value(this.state.shiftedBy(this.duration + 10.));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the intervals don't cover the given date (open interval)
        try {
            fct.value(this.state);
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }
        try {
            fct.value(this.state.shiftedBy(this.duration));
            Assert.fail();
        } catch (final IllegalStateException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When the intervals list isn't the same size as the sub-functions list
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(this.duration * 5), date
                .shiftedBy(this.duration * 6)));
        try {
            new IntervalsFunction(cstFcts, this.intervals);
            Assert.fail();
        } catch (final DimensionMismatchException e) {
            // expected
            Assert.assertTrue(true);
        }

        // When some intervals overlap with each other (even when two intervals end-points are
        // closed)
        this.intervals = new ArrayList<>();
        this.intervals.add(new AbsoluteDateInterval(date, date.shiftedBy(this.duration)));
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(this.duration), date
                .shiftedBy(this.duration * 2)));
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(this.duration * 2), date
                .shiftedBy(this.duration * 3)));
        try {
            new IntervalsFunction(cstFcts, this.intervals);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        // Check to build an intervals function with immutable parameters
        this.intervals = new ArrayList<>();
        this.intervals.add(new AbsoluteDateInterval(date, date.shiftedBy(this.duration)));
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(this.duration * 2), date
                .shiftedBy(this.duration * 3)));
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(this.duration * 4), date
                .shiftedBy(this.duration * 5)));

        final Parameter param1 = new Parameter("param1", 1.);
        final Parameter param2 = new Parameter("param2", 1.);
        final Parameter param3 = new Parameter("param3", 1.);

        // Only set in final state the parameters 1 & 3
        param1.getDescriptor().setMutability(false);
        param3.getDescriptor().setMutability(false);

        cstFcts = new ArrayList<>();
        cstFcts.add(new ConstantFunction(param1));
        cstFcts.add(new ConstantFunction(param2));
        cstFcts.add(new ConstantFunction(param3));

        fct = new IntervalsFunction(cstFcts, this.intervals);

        // The parameters 1 & 3 shouldn't have been updated by the interval parameter descriptor,
        // unlike the parameters 2
        Assert.assertFalse(fct.getParameters().get(0).toString().contains("2000"));
        Assert.assertTrue(fct.getParameters().get(1).toString().contains("2000"));
        Assert.assertFalse(fct.getParameters().get(2).toString().contains("2000"));

        // Try to use overlapping intervals
        this.intervals
                .add(new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date
                        .shiftedBy(this.duration * 5), date.shiftedBy(this.duration * 6),
                        IntervalEndpointType.CLOSED));

        params = new Parameter[this.intervals.size()];
        for (int i = 0; i < this.intervals.size(); i++) {
            params[i] = new Parameter("cf " + i, i + 0.5);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
            mapOfFunctions.put(this.intervals.get(i), cstFunc);
        }

        try {
            new IntervalsFunction(cstFcts, this.intervals);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

        try {
            new IntervalsFunction(mapOfFunctions);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * Global parameters initialization.
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {

        this.duration = 4 * 3600.0;

        // Initial state initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU);
        this.state = new SpacecraftState(orbit);

        // Piecewise function initialization
        final double duration = 4 * 3600.0;
        final int nbPieces = 4;
        final List<AbsoluteDate> dates = new ArrayList<>();
        final Parameter[] params = new Parameter[nbPieces];
        final List<IParamDiffFunction> cstFcts = new ArrayList<>();
        for (int i = 0; i < nbPieces; i++) {
            final AbsoluteDate currentDate = date.shiftedBy(duration * (i + 1));
            dates.add(currentDate);
            params[i] = new Parameter("cf " + i, +1.0 * i);
            final ConstantFunction cstFunc = new ConstantFunction(params[i]);
            cstFcts.add(cstFunc);
        }
        dates.remove(dates.size() - 1);
        this.piecewiseFct = new PiecewiseFunction(cstFcts, dates);

        // Describe the followings intervals (duration = d):
        // ]date; date+d[ [date+2d; d+3d[ [date+3d; date+4d] ]date+4d; date+5d]
        this.intervals = new ArrayList<>();
        this.intervals.add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date, date
                .shiftedBy(duration), IntervalEndpointType.OPEN));
        this.intervals.add(new AbsoluteDateInterval(IntervalEndpointType.CLOSED, date
                .shiftedBy(duration * 2), date.shiftedBy(duration * 3), IntervalEndpointType.OPEN));
        this.intervals.add(new AbsoluteDateInterval(date.shiftedBy(duration * 3), date
                .shiftedBy(duration * 4)));
        this.intervals
                .add(new AbsoluteDateInterval(IntervalEndpointType.OPEN, date
                        .shiftedBy(duration * 4), date.shiftedBy(duration * 5),
                        IntervalEndpointType.CLOSED));
    }
}
