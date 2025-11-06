package fr.cnes.sirius.patrius.covariance;

/** HISTORY
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * END-HISTORY
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.FieldDescriptor;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.SpacecraftStateProvider;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BasicMultiOrbitalCovarianceProviderTest extends AbstractBasicOrbitalCovarianceProviderTest {

    /** Satellite orbits */
    final private PVCoordinates sat1PVCoords =
        new PVCoordinates(7179992.82, 2276.519, -14178.396, 7.450848, -1181.198684, 7356.62864);
    final private PVCoordinates sat2PVCoords =
        new PVCoordinates(-14178.396, 7179992.82, 2276.519, 7356.62864, 7.450848, -1181.198684);
    final private PVCoordinates sat3PVCoords =
        new PVCoordinates(2276.519, -14178.396, 7179992.82, -1181.198684, 7356.62864, 7.450848);

    /** Reference date for propagations */
    final private AbsoluteDate refDate = new AbsoluteDate(10, 0.0);

    /** Propagator with hardcoded date values so no propagation is needed */
    private List<BoundedPropagator> fixedStatePropagators;

    /**
     * @throws PatriusException,
     *         IOException
     * @testType VT
     *
     * @testedFeature The results of propagating the covariance using the newly implemented
     *                {@link BasicMultiOrbitalCovarianceProvider#getMultiOrbitalCovariance(AbsoluteDate)} for 3
     *                satellites at the same time are the same as 3 BasicOrbitalCovarianceProvider propagated
     *                independently.
     *
     * @description An BasicMultiOrbitalCovarianceProvider object is created containing the covariance information for 3
     *              different satellites. Then, 3 independent BasicOrbitalCovarianceProvider objects are created, one
     *              per satellite. It is then checked that the results after the propagation match.
     *
     * @input Covariance information for 3 satellites.
     *
     * @output the propagated covariance using the BasicMultiOrbitalCovarianceProvider and the 3
     *         BasicOrbitalCovarianceProvider
     *
     * @testPassCriteria the submatrices corresponding to each satellite from the BasicMultiOrbitalCovarianceProvider
     *                   match the corresponding BasicOrbitalCovarianceProvider matrices
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testCompareMultiVsMonoBasicOrbitalCov() throws PatriusException {

        // Initialize the numerical propagator
        buildNumericalPropagator(this.sat1PVCoords, 0);
        buildNumericalPropagator(this.sat2PVCoords, 1);
        buildNumericalPropagator(this.sat3PVCoords, 2);

        // Create covariance matrix
        final Covariance covariance = createGlobalCovariance(Arrays.asList(false, false, false));

        // No need to use fixed state propagator since comparing one object against the other but all propagated with
        // the same numerical propagator.
        final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
        for (int i = 0; i < this.mapper.size(); i++) {
            map.put(this.ephemeris.get(i), this.mapper.get(i));
        }

        // Build basic multi orbital covariance provider
        final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
            new BasicMultiOrbitalCovarianceProvider(covariance, map);

        // Build 3 basic orbital covariance providers
        // Build basic orbital covariance provider
        final BasicOrbitalCovarianceProvider basicOrbCovProv0 =
            new BasicOrbitalCovarianceProvider(covariance.getSubCovariance(IntStream.range(0, 7).toArray()),
                this.ephemeris.get(0), this.mapper.get(0));

        final BasicOrbitalCovarianceProvider basicOrbCovProv1 =
            new BasicOrbitalCovarianceProvider(covariance.getSubCovariance(IntStream.range(7, 14).toArray()),
                this.ephemeris.get(1), this.mapper.get(1));

        final BasicOrbitalCovarianceProvider basicOrbCovProv2 =
            new BasicOrbitalCovarianceProvider(covariance.getSubCovariance(IntStream.range(14, 21).toArray()),
                this.ephemeris.get(2), this.mapper.get(2));

        // Propagate covariances
        final MultiOrbitalCovariance multiCov = multiOrbCovProv.getMultiOrbitalCovariance(this.refDate);
        final OrbitalCovariance monoCov0 =
            multiOrbCovProv.getOrbitalCovarianceProvider(0).getOrbitalCovariance(this.refDate);
        final OrbitalCovariance monoCov1 =
            multiOrbCovProv.getOrbitalCovarianceProvider(1).getOrbitalCovariance(this.refDate);
        final OrbitalCovariance monoCov2 =
            multiOrbCovProv.getOrbitalCovarianceProvider(2).getOrbitalCovariance(this.refDate);

        final OrbitalCovariance monoCov0FromMono = basicOrbCovProv0.getOrbitalCovariance(this.refDate);
        final OrbitalCovariance monoCov1FromMono = basicOrbCovProv1.getOrbitalCovariance(this.refDate);
        final OrbitalCovariance monoCov2FromMono = basicOrbCovProv2.getOrbitalCovariance(this.refDate);

        // Check results
        Assert.assertTrue(
            matrixEquals(monoCov0FromMono.getCovarianceMatrix(), monoCov0.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

        Assert.assertTrue(
            matrixEquals(monoCov1FromMono.getCovarianceMatrix(), monoCov1.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

        Assert.assertTrue(
            matrixEquals(monoCov2FromMono.getCovarianceMatrix(), monoCov2.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

        Assert.assertTrue(
            matrixEquals(multiCov.getCovarianceMatrix().getSubMatrix(0, 6, 0, 6), monoCov0.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

        Assert.assertTrue(
            matrixEquals(multiCov.getCovarianceMatrix().getSubMatrix(7, 13, 7, 13), monoCov1.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

        Assert.assertTrue(
            matrixEquals(multiCov.getCovarianceMatrix().getSubMatrix(14, 20, 14, 20), monoCov2.getCovarianceMatrix(),
                Precision.DOUBLE_COMPARISON_EPSILON,
                Precision.DOUBLE_COMPARISON_EPSILON, true));

    }

    /**
     * @throws PatriusException,
     *         IOException
     * @testType UT
     *
     * @testedFeature Non regression test for BasicMultiOrbitalCovarianceProvider.
     *
     * @description A multi covariance for 3 different satellites is propagated. For 2 of the satellites, only the
     *              covariance of the orbital parameters is considered. For the remaining satellite, also the Drag is
     *              taken into account.
     *              
     *              A fixed-state propagator is used to avoid numerical differences in future evolutions of the
     *              numerical propagator.
     *
     * @input Covariance information for 3 satellites.
     *
     * @output the propagated covariance using the BasicMultiOrbitalCovarianceProvider
     *
     * @testPassCriteria the propagated matrix matches the reference.
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testMultiCovariancePropagation() throws PatriusException {

        // Initialize mapper
        initializeMapperForFixedPropagator(0);
        initializeMapperForFixedPropagator(1);
        initializeMapperForFixedPropagator(2);

        // Create covariance matrix
        final List<Boolean> isOnlyOrbital = Arrays.asList(true, false, true);
        final Covariance covariance = createGlobalCovariance(isOnlyOrbital);

        final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
        for (int i = 0; i < this.mapper.size(); i++) {
            map.put(this.fixedStatePropagators.get(i), this.mapper.get(i));
        }

        // Build basic multi orbital covariance provider
        final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
            new BasicMultiOrbitalCovarianceProvider(covariance, map);

        // Propagate covariances
        final MultiOrbitalCovariance multiCov = multiOrbCovProv.getMultiOrbitalCovariance(this.refDate);

        final double[][] refMat = {
            { 7131.05113553547, 3562.140478004452, 84.59812987473164, 169.16636325138163, 84.33987952265701,
                0.012089421312954169, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 3562.140478004452, 7117.384087398323, 3558.770350565186, 84.50662716649154, 168.51899393938882,
                84.26679464748878, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 84.59812987473164, 3558.770350565186, 7117.415141139849, 2.0170569322206395, 84.26168792252638,
                168.5209129061674, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 169.16636325138163, 84.50662716649154, 2.0170569322206395, 4.015292842184757, 2.0019644940974537,
                5.008745412959463E-4, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 84.33987952265701, 168.51899393938882, 84.26168792252638, 2.0019644940974537, 3.99228859528734,
                1.996325479030908, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.012089421312954169, 84.26679464748878, 168.5209129061674, 5.008745412959463E-4, 1.996325479030908,
                3.9923565315819696, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 14235.092946217446, 7124.770990762576, 168.6184401450991, 337.057783896188,
                169.03782441736803, -0.007030175309357778, -6.068722326948356E-4, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7124.770990762576, 14262.358407286461, 7124.1498228108285,
                168.7145046973989, 338.34834085631604, 168.67653024966214, -5.013645472995112E-4, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 168.6184401450991, 7124.1498228108285, 14234.773223557278,
                4.004113210727234, 169.00054610018964, 337.0382060128243, 168.68220815811333, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 337.057783896188, 168.7145046973989, 4.004113210727234, 7.98533247357106,
                4.0050596079367144, 5.70909484575521E-5, -2.9697068607111163E-5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 169.03782441736803, 338.34834085631604, 169.00054610018964,
                4.0050596079367144, 8.031182278663795, 4.0036196679337, -4.8924885647411285E-5, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.007030175309357778, 168.67653024966214, 337.0382060128243,
                5.70909484575521E-5, 4.0036196679337, 7.984584748906057, 3.9961724854976883, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.068722326948356E-4, -5.013645472995112E-4, 168.68220815811333,
                -2.9697068607111163E-5, -4.8924885647411285E-5, 3.9961724854976883, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 21352.213903734333, 10676.087587833123,
                253.5790434832756, 505.56073842182406, 252.7807660895456, 0.03468968748209997 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10676.087587833123, 21352.637907801643,
                10687.126441283275, 252.77745955809291, 505.5865647147026, 253.55372896942205 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 253.5790434832756, 10687.126441283275,
                21393.60067306857, 6.002592236985466, 253.07112200048766, 507.52634054041204 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 505.56073842182406, 252.77745955809291,
                6.002592236985466, 11.977009098346688, 5.988455918673127, 7.071092300638213E-4 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 252.7807660895456, 505.5865647147026,
                253.07112200048766, 5.988455918673127, 11.977994295972954, 6.00752006716127 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.03468968748209997, 253.55372896942205,
                507.52634054041204, 7.071092300638213E-4, 6.00752006716127, 12.04692224115332 },
        };

        final SymmetricPositiveMatrix refCov = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, refMat);

        // Check results
        Assert.assertTrue(matrixEquals(multiCov.getCovarianceMatrix(), refCov,
            Precision.DOUBLE_COMPARISON_EPSILON, Precision.DOUBLE_COMPARISON_EPSILON, true));
    }

    /**
     * @throws PatriusException,
     *         IOException
     * @testType UT
     *
     * @testedFeature The robustness of the class {@link BasicMultiOrbitalCovarianceProvider}
     *
     * @description The BasicMultiOrbitalCovarianceProvider object is constructed with different issues. First, a
     *              covariance without parameter descriptors is build. Second, a covariance with the orbital parameter
     *              descriptors in the wrong order.
     *
     * @input The different covariance configurations
     *
     * @output the expected errors
     *
     * @testPassCriteria the expected errors are raised
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testErrorsOrbCoord() throws PatriusException {

        // Initialize the numerical propagator
        buildNumericalPropagator(this.sat1PVCoords, 0);
        buildNumericalPropagator(this.sat2PVCoords, 1);
        buildNumericalPropagator(this.sat3PVCoords, 2);

        // Create covariance matrix
        final List<Boolean> isOnlyOrbital = Arrays.asList(true, false, true);
        final Covariance covariance = createGlobalCovariance(isOnlyOrbital);

        final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
        for (int i = 0; i < this.mapper.size(); i++) {
            map.put(this.ephemeris.get(i), this.mapper.get(i));
        }

        // Build BasicMultiOrbitalCovarianceProvider without descriptors
        try {
            final Covariance newCov = new Covariance(covariance.getCovarianceMatrix());

            // Build basic multi orbital covariance provider
            final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
                new BasicMultiOrbitalCovarianceProvider(newCov, map);

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage =
                "The descriptor (0) of spacecraft (0) of the covariance (ParameterDescriptor[parameter_name: p0]) is not an orbital parameter";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

        // Build BasicMultiOrbitalCovarianceProvider with orbital descriptors in the wrong order
        try {
            final List<ParameterDescriptor> newParamDesc = new ArrayList<>();
            final List<ParameterDescriptor> oldParamDesc = covariance.getParameterDescriptors();

            newParamDesc.add(oldParamDesc.get(1));
            newParamDesc.add(oldParamDesc.get(0));

            for (int i = 2; i < oldParamDesc.size(); i++) {
                newParamDesc.add(oldParamDesc.get(i));
            }

            final Covariance newCov = new Covariance(covariance.getCovarianceMatrix(), newParamDesc);

            // Build basic orbital covariance provider
            final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
                new BasicMultiOrbitalCovarianceProvider(newCov, map);

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage =
                "The orbital parameters of the spacecraft (0) of the covariance are not in the correct order";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

    }

    /**
     * @throws PatriusException,
     *         IOException
     * @testType UT
     *
     * @testedFeature The coherence of the JacobiansMapper of each satellite
     *
     * @description The NumericalPropagators of different satellites that will be part of a
     *              BasicMultiOrbitalCovarianceProvider are configured so that they present some incongruences
     *              (different orbitType, different PositionAngle, different Frame). A different error is raised in each
     *              case.
     *
     * @input The different NumericalPropagators configurations
     *
     * @output the expected errors
     *
     * @testPassCriteria the expected errors are raised
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testErrorsMapper() throws PatriusException {

        // Initialize the numerical propagator so that they are DIFFERENT
        buildNumericalPropagator(this.sat1PVCoords, 0, OrbitType.CARTESIAN, PositionAngle.TRUE,
            FramesFactory.getGCRF());
        buildNumericalPropagator(this.sat2PVCoords, 1, OrbitType.CARTESIAN, PositionAngle.MEAN,
            FramesFactory.getGCRF());
        buildNumericalPropagator(this.sat3PVCoords, 2, OrbitType.CARTESIAN, PositionAngle.MEAN,
            FramesFactory.getICRF());
        buildNumericalPropagator(this.sat3PVCoords, 3, OrbitType.KEPLERIAN, PositionAngle.MEAN,
            FramesFactory.getICRF());

        // Create covariance matrix
        final List<Boolean> isOnlyOrbital = Arrays.asList(true, true);
        final Covariance covariance = createGlobalCovariance(isOnlyOrbital);

        // Test an error is raised because different PositionAngles
        try {

            // Take the two with different position angles
            final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
            map.put(this.ephemeris.get(0), this.mapper.get(0));
            map.put(this.ephemeris.get(1), this.mapper.get(1));

            // Build basic multi orbital covariance provider
            final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
                new BasicMultiOrbitalCovarianceProvider(covariance, map);

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage = "The mappers position angles are not all identical.";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

        // Test an error is raised because different Frames
        try {

            // Take the two with different frames
            final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
            map.put(this.ephemeris.get(1), this.mapper.get(1));
            map.put(this.ephemeris.get(2), this.mapper.get(2));

            // Build basic multi orbital covariance provider
            final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
                new BasicMultiOrbitalCovarianceProvider(covariance, map);

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage = "The mappers frames are not all identical.";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

        // Test an error is raised because different orbit types
        try {

            // Take the two with different frames
            final LinkedHashMap<SpacecraftStateProvider, JacobiansMapper> map = new LinkedHashMap<>();
            map.put(this.ephemeris.get(2), this.mapper.get(2));
            map.put(this.ephemeris.get(3), this.mapper.get(3));

            // Build basic multi orbital covariance provider
            final BasicMultiOrbitalCovarianceProvider multiOrbCovProv =
                new BasicMultiOrbitalCovarianceProvider(covariance, map);

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage = "The mappers orbit types are not all identical.";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

    }

    /**
     * Create a global covariance matrix for nSatellites
     *
     * @param isOnlyOrbital
     * @return
     */
    private Covariance createGlobalCovariance(final List<Boolean> isOnlyOrbital) {

        // Complete matrix
        RealMatrix globalCovMatrix = null;
        for (int sat = 0; sat < isOnlyOrbital.size(); sat++) {

            RealMatrix covMatrix =
                new Array2DRowRealMatrix(new double[][] {
                    { 4.0, 2.0, 2.0, 0.0, 0.0, 0.0, 0.0 },
                    { 2.0, 4.0, 2.0, 0.0, 0.0, 0.0, 0.0 },
                    { 0.0, 2.0, 4.0, 2.0, 0.0, 0.0, 0.0 },
                    { 0.0, 0.0, 2.0, 4.0, 2.0, 0.0, 0.0 },
                    { 0.0, 0.0, 0.0, 2.0, 4.0, 2.0, 0.0 },
                    { 0.0, 0.0, 0.0, 0.0, 2.0, 4.0, 2.0 },
                    { 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 4.0 } });

            covMatrix = covMatrix.scalarMultiply(sat + 1);

            // Limit to orbital elements
            if (isOnlyOrbital.get(sat)) {
                covMatrix = covMatrix.getSubMatrix(0, 5, 0, 5);
            }

            final ArrayRowSymmetricPositiveMatrix mat =
                new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, covMatrix);

            if (sat == 0) {
                globalCovMatrix = mat;
            } else {
                globalCovMatrix = globalCovMatrix.concatenateDiagonally(mat);
            }

        }

        // Parameter descriptors
        final List<ParameterDescriptor> paramDesc = new ArrayList<>();
        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = new LinkedHashMap<>();

        for (int sat = 0; sat < isOnlyOrbital.size(); sat++) {

            // Orbital parameter descriptors
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.X);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "X_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            fieldDescriptorsMap.clear();
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.Y);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "Y_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            fieldDescriptorsMap.clear();
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.Z);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "Z_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            fieldDescriptorsMap.clear();
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VX);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "VX_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            fieldDescriptorsMap.clear();
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VY);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "VY_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            fieldDescriptorsMap.clear();
            fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VZ);
            fieldDescriptorsMap.put(StandardFieldDescriptors.PARAMETER_NAME, "VZ_" + sat);
            paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            if (!isOnlyOrbital.get(sat)) {
                // Drag parameter descriptor from the propagator
                for (final Parameter param : this.mapper.get(sat).getParametersList()) {
                    if (param.getName().contains("DragForce")) {
                        paramDesc.add(param.getDescriptor());
                    }
                }
            }
        }

        // Return object
        return new Covariance(new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, globalCovMatrix), paramDesc);
    }

    @Before
    public void setUpTest() throws PatriusException {

        // initialize class variables
        this.ephemeris = new ArrayList<>();
        this.mapper = new ArrayList<>();
        this.fixedStatePropagators = new ArrayList<>();

        // Number of points computed per satellite to create each fixed scStateProvider
        final int pointsPerSat = 2;

        // Uncomment this part of code if we want to recompute the fixed ephemeris for the scStates
        // Create fixed spacecraft states so no propagation is required
        // try {
        //
        // // Initialize the numerical propagator
        // buildNumericalPropagator(this.sat1PVCoords, 0);
        // buildNumericalPropagator(this.sat2PVCoords, 1);
        // buildNumericalPropagator(this.sat3PVCoords, 2);
        //
        // // Propagate also for this.refDate.shiftedBy(10) to create two points;
        // for (int i = 0; i < pointsPerSat; i++) {
        // final PVCoordinates pvCoord0 =
        // this.ephemeris.get(0).getSpacecraftState(this.refDate.shiftedBy(10 * i)).getPVCoordinates();
        // final PVCoordinates pvCoord1 =
        // this.ephemeris.get(1).getSpacecraftState(this.refDate.shiftedBy(10 * i)).getPVCoordinates();
        // final PVCoordinates pvCoord2 =
        // this.ephemeris.get(2).getSpacecraftState(this.refDate.shiftedBy(10 * i)).getPVCoordinates();
        // }
        //
        // } catch (final Exception e) {
        // // DUMMY
        // }

        // Coords
        // Initialize fixed pvCoords
        final List<PVCoordinates> pvCoords = new ArrayList<>();

        // First point for each one of the three propagators
        pvCoords.add(new PVCoordinates(7173428.390261228, -47537.43371475313, 296068.0990216858, -318.6366940231377,
            -1180.1703980868872, 7350.224384997146));
        pvCoords.add(new PVCoordinates(296068.0989983038, 7173428.3902719915, -47537.4337686825, 7350.224383888783,
            -318.6366935125134, -1180.170400643341));
        pvCoords.add(new PVCoordinates(-47537.433779566185, 296068.0990530504, 7173428.390295247, -1180.1704011607617,
            7350.22438649139, -318.6366924071606));

        // Second point for each one of the three propagators
        pvCoords.add(new PVCoordinates(7169855.786896824, -59336.365926371385, 369553.0800236523, -395.87796620669906,
            -1179.5948615857258, 7346.639887635129));
        pvCoords.add(new PVCoordinates(369553.07998787484, 7169855.786913308, -59336.366008893136, 7346.639886264247,
            -395.8779655748333, -1179.5948647476662));
        pvCoords.add(new PVCoordinates(-59336.36602545611, 369553.08007119, 7169855.786948684, -1179.5948653781336,
            7346.6398894356535, -395.87796422783754));

        // Additional states
        // Initialize additional states. First for the three propagators
        final List<double[]> addStates = new ArrayList<>();
        addStates.add(new double[] { 1.0019162125514756, -5.735647579735861E-6, 3.572220764538264E-5, 42.21093249629911,
            -1.2738612188434604E-4, 7.933739431771055E-4, -5.739480591438897E-6, 0.9990419908958477,
            -1.192106414684279E-7, -1.274131532177687E-4, 42.17052817837113, -3.1058665017860065E-6,
            3.5746065568554045E-5, -1.1921058403651892E-7, 0.9990427142110498, 7.935420936999662E-4,
            -3.1058652318283643E-6, 42.170547027804865, 9.084164321402429E-5, -4.293799085785096E-7,
            2.6742226851142306E-6, 1.0019146279491788, -1.2380155035720068E-5, 7.710488899756821E-5,
            -4.2983341483882147E-7, -4.541324236315908E-5, -1.1839458458403297E-8, -1.23839882710319E-5,
            0.9990419903789084, -3.8019461426344855E-7, 2.677046483363784E-6, -1.1839454179097341E-8,
            -4.534140597975231E-5, 7.71287483250404E-5, -3.8019449110160346E-7, 0.9990442974365937,
            1.0224741312929286E-6, -5.383411315366404E-6, 1.1930672900670103E-5, 2.6964717600399826E-5,
            -7.430547563085251E-5, 1.169045609703981E-5, 7.446516025992503E-8, -2.553118935552395E-7,
            5.654998398816466E-7, 1.2782291656427795E-6, -3.5219920065735255E-6, 5.541675938966316E-7 });
        addStates.add(new double[] { 0.9990427142111562, 3.574607118182749E-5, -1.1921059916366362E-7,
            42.17054702639231, 7.935421900254164E-4, -3.1065332600891402E-6, 3.572220842949835E-5, 1.0019162125513827,
            -5.7356468361306835E-6, 7.933739536114047E-4, 42.21093249573137, -1.2738610022360446E-4,
            -1.192105895244912E-7, -5.7394779118139155E-6, 0.9990419908958479, -3.1065330468351076E-6,
            -1.2741310723635485E-4, 42.1705281780904, -4.534140597241946E-5, 2.677046749383082E-6,
            -1.1839455233853897E-8, 0.999044297369763, 7.712875462919186E-5, -3.8022617396252764E-7,
            2.6742227221442707E-6, 9.084164320711954E-5, -4.29379873473863E-7, 7.710489047118922E-5, 1.0019146279221791,
            -1.2380153597791004E-5, -1.1839454515369036E-8, -4.298332879799436E-7, -4.541324236312269E-5,
            -3.802261532770847E-7, -1.238398489948796E-5, 0.9990419903655945, -7.430576245680987E-5, 0.0,
            1.0224816993633292E-6, 0.0, 1.1930718964350177E-5, 0.0, -3.5220204889076315E-6, 0.0, 7.446609296909097E-8,
            0.0, 5.655044138007712E-7, 0.0 });
        addStates.add(new double[] { 0.999041990895723, -1.192113215793223E-7, -5.7394772607190056E-6,
            42.170528178933374, -3.1065782974588254E-6, -1.2741309494476434E-4, -1.1920914669942122E-7,
            0.9990427142111358, 3.574606422019952E-5, -3.1065477246570186E-6, 42.170547028111066, 7.935420666388994E-4,
            -5.735646847090314E-6, 3.5722207646731916E-5, 1.0019162125515046, -1.2738610298271838E-4,
            7.933739499768615E-4, 42.210932496589805, -4.541324236933223E-5, -1.1839489049385106E-8,
            -4.298332558810299E-7, 0.9990419904055837, -3.802287578494563E-7, -1.2383984158893025E-5,
            -1.1839386024775416E-8, -4.534140597590906E-5, 2.6770464197668E-6, -3.8022658535422294E-7,
            0.9990442974510253, 7.712874687265871E-5, -4.2937987428786533E-7, 2.674222685240146E-6,
            9.084164321611502E-5, -1.2380153583790129E-5, 7.710488888451145E-5, 1.0019146279629643,
            1.174991350738606E-5, -5.3514027383045716E-6, -7.317968720579174E-5, 2.680957797157198E-5,
            1.0069901981084768E-6, 1.1630515336015344E-5, 5.569369319397306E-7, -2.5442673805759793E-7,
            -3.4686613179192303E-6, 1.2746243184414583E-6, 7.333826809200007E-8, 5.532398559175793E-7 });

        // Initialize additional states. Second for the three propagators
        addStates.add(new double[] { 1.0029322403904097, -1.1185913932937043E-5, 6.966703308298211E-5,
            52.23496959823537, -3.039964015894693E-4, 0.0018933210881250696, -1.1197001981891432E-5, 0.9985340759122446,
            -2.8945359751406203E-7, -3.0409300440053643E-4, 52.15849822168866, -9.281839748863865E-6,
            6.973606846243579E-5, -2.894534858189128E-7, 0.9985358321833743, 0.0018939223555135953,
            -9.281836723124687E-6, 52.158554546455434, 1.1236242860881762E-4, -6.694840314785775E-7,
            4.169616170899738E-6, 1.0029284177495175, -2.3758503126620117E-5, 1.4797042367520682E-4,
            -6.705453288953467E-7, -5.616876664833192E-5, -2.3029272799345662E-8, -2.376959197538025E-5,
            0.9985340959749025, -9.12439310716011E-7, 4.1762251934883045E-6, -2.3029266139048756E-8,
            -5.602903561269407E-5, 1.480394640642293E-4, -9.124390746302798E-7, 0.9985396324989736,
            1.9621872477233454E-6, -8.239586972464068E-6, 1.8255002938941842E-5, 4.1260864534878136E-5,
            -1.1369406299071398E-4, 1.7888419222128812E-5, 1.1492790491622604E-7, -3.159320512621472E-7,
            6.993451175937994E-7, 1.5809746735650588E-6, -4.355594362771158E-6, 6.854129241300521E-7 });
        addStates.add(new double[] { 0.9985358321835573, 6.973607704999606E-5, -2.8945351161078314E-7,
            52.158554544297004, 0.0018939225140297096, -9.282859315366297E-6, 6.966703428090508E-5, 1.0029322403902277,
            -1.1185912797649344E-5, 0.0018933211387592314, 52.23496959736572, -3.039963653426398E-4,
            -2.8945349286056875E-7, -1.1196997884353656E-5, 0.9985340759122461, -9.282858807207468E-6,
            -3.040929133524518E-4, 52.15849822125908, -5.602903560128257E-5, 4.176225522410982E-6,
            -2.3029267800449652E-8, 0.9985396324164386, 1.4803947371920903E-4, -9.124782751971408E-7,
            4.169616216458261E-6, 1.1236242859808726E-4, -6.694839882408993E-7, 1.479704259388948E-4,
            1.0029284177159488, -2.3758500925881692E-5, -2.3029266682130753E-8, -6.705451722118944E-7,
            -5.616876664826349E-5, -9.124782355423333E-7, -2.3769586817966292E-5, 0.998534095958424,
            -1.1369475931743636E-4, 0.0, 1.9622100182378064E-6, 0.0, 1.8255114762183623E-5, 0.0, -4.355649750444434E-6,
            0.0, 1.1493015582506466E-7, 0.0, 6.993540118852905E-7, 0.0 });
        addStates.add(new double[] { 0.9985340759120498, -2.8945461330828055E-7, -1.1196996869324886E-5,
            52.15849822254925, -9.282934907435368E-6, -3.040928949120797E-4, -2.894512866170051E-7, 0.998535832183505,
            6.973606640439637E-5, -9.282877068250123E-6, 52.158554546922446, 0.001893922314122883,
            -1.118591281891066E-5, 6.96670330860851E-5, 1.0029322403904646, -3.039963635662407E-4, 0.001893321087992382,
            52.23496959868034, -5.616876665612287E-5, -2.302930911950296E-8, -6.705451314411511E-7, 0.9985340960079119,
            -9.124818638974101E-7, -2.376958563947166E-5, -2.302918179375248E-8, -5.6029035608320385E-5,
            4.176225115162311E-6, -9.124785429830983E-7, 0.9985396325167625, 1.4803946185436658E-4,
            -6.694839895424444E-7, 4.169616171192572E-6, 1.1236242861184806E-4, -2.375850090569027E-5,
            1.4797042350604928E-4, 1.0029284177666065, 1.7978509100667046E-5, -8.143220399759868E-6,
            -1.1197203039609705E-4, 4.079598559231369E-5, 1.932489496706325E-6, 1.770284591606302E-5,
            6.887621006226619E-7, -3.0993766510646805E-7, -4.289682221873305E-6, 1.5527195661893947E-6,
            1.1319033863184447E-7, 6.743669416446419E-7 });

        // 3 satellites, 2 points per satellite
        final int maxSat = 3;
        for (int sat = 0; sat < 3; sat++) {
            final List<SpacecraftState> scStates = new ArrayList<>();
            for (int pointPerSat = 0; pointPerSat < pointsPerSat; pointPerSat++) {
                // Create fixed scState 0
                final CartesianOrbit orbit =
                    new CartesianOrbit(pvCoords.get(sat + maxSat * pointPerSat), FramesFactory.getGCRF(),
                        this.refDate.shiftedBy(10 * pointPerSat), Constants.EGM96_EARTH_MU);
                final SpacecraftState scState = new SpacecraftState(orbit);
                scStates.add(scState.addAdditionalState("PDE", addStates.get(sat + maxSat * pointPerSat)));
            }

            // Create fixed scState propagator
            this.fixedStatePropagators.add(new Ephemeris(scStates, 2));
            this.fixedStatePropagators.get(sat).setOrbitFrame(FramesFactory.getGCRF());
        }

    }

}
