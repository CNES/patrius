package fr.cnes.sirius.patrius.covariance;

/** HISTORY
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * END-HISTORY
 */
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
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
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.precomputed.Ephemeris;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BasicOrbitalCovarianceProviderTest extends AbstractBasicOrbitalCovarianceProviderTest {

    /** Propagator with hardcoded date values so no propagation is needed */
    private BoundedPropagator fixedStatePropagator;

    /**
     * @throws PatriusException,
     *         IOException
     * @testType VT
     *
     * @testedFeature The results of propagating the covariance using the newly implemented
     *                {@link BasicOrbitalCovarianceProvider#getOrbitalCovariance(AbsoluteDate)} are the same as the ones
     *                obtained using the Ficus class
     *
     * @description The Ficus test CovarianceAnalysisTest#testExternalValidationCovarianceAnalysisPropagation is taken
     *              as reference.
     *              The initial covariance matrix is taken from the
     *              covAnal.getLowLevelCovarianceAnalysis().getCovFromMeasNoiseMatrix() object from that test. Only the
     *              elements regarding the orbital coordinates or the propagation forces are copied.
     *              The spacecraftStateProvides is also taken from the Ficus test. The references are also taken from
     *              the ficus test.
     *
     *              A fixed-state propagator is used to avoid numerical differences in future evolutions of the
     *              numerical propagator.
     *
     * @input The initial values for the elements are taken from the Ficus test
     *        CovarianceAnalysisTest#testExternalValidationCovarianceAnalysisPropagation
     *
     * @output the covariance at different dates
     *
     * @testPassCriteria the covariance at the different dates is within tolerance of the Ficus base results
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testExternalValidationCovarianceAnalysisPropagation() throws PatriusException {

        final double absoluteTolerance = 9.75e-12;
        final double relativeTolerance = 9.75e-12;

        // Initialize the numerical propagator
        initializeMapperForFixedPropagator(1);

        // Create covariance matrix
        final Covariance covariance = createInitialCovariance(false);

        // Build basic orbital covariance provider
        final BasicOrbitalCovarianceProvider basicOrbCovProv =
            new BasicOrbitalCovarianceProvider(covariance, this.fixedStatePropagator, this.mapper.get(0));

        // Check results
        for (final Entry<AbsoluteDate, SymmetricPositiveMatrix> ref : getReferences().entrySet()) {

            final AbsoluteDate refDate = ref.getKey();
            final SymmetricPositiveMatrix refMat = ref.getValue();

            final SymmetricPositiveMatrix satCovMatrixFromMonoMeas =
                basicOrbCovProv.getOrbitalCovariance(refDate).getCovarianceMatrix();

            Assert.assertTrue(
                matrixEquals(satCovMatrixFromMonoMeas, refMat, relativeTolerance, absoluteTolerance, true));
        }
    }

    /**
     * @throws PatriusException,
     *         IOException
     * @testType UT
     *
     * @testedFeature Non regression of
     *                {@link BasicOrbitalCovarianceProvider#getOrbitalCovariance(AbsoluteDate)}.
     *
     * @description The non regression when computing the propagated orbital covariance is tested.
     *              A fixed-state propagator is used to avoid numerical differences in future
     *              evolutions of the numerical propagator.
     *
     * @input The initial values for the elements are taken from the Ficus test
     *        CovarianceAnalysisTest#testExternalValidationCovarianceAnalysisPropagation
     *
     * @output the covariance at one date
     *
     * @testPassCriteria the propagated covariance matches the reference
     *
     * @referenceVersion 4.16
     *
     * @nonRegressionVersion 4.16
     */
    @Test
    public void testOrbitalCovarianceAnalysisPropagation() throws PatriusException {

        final double absoluteTolerance = Precision.DOUBLE_COMPARISON_EPSILON;
        final double relativeTolerance = Precision.DOUBLE_COMPARISON_EPSILON;

        // Initialize the numerical propagator
        initializeMapperForFixedPropagator(1);

        // Create covariance matrix
        final Covariance covariance = createInitialCovariance(true);

        // Build basic orbital covariance provider
        final BasicOrbitalCovarianceProvider basicOrbCovProv =
            new BasicOrbitalCovarianceProvider(covariance, this.fixedStatePropagator, this.mapper.get(0));

        // Check results
        // 10th ref
        final AbsoluteDate refDate = new AbsoluteDate(12, 0.8160000000000025);

        // Reference matrix
        final double[][] mat1 = {
            { 0.3384649033492238, -1.3583030101695692E-4, -0.005677455049018714, 0.010597441416485083,
                -3.7857001323432846E-5, -4.6627424747658885E-5 },
            { -1.3583030101695692E-4, 0.5101263177262041, -1.0733550709997748, -2.1812502831716207E-4,
                0.014886082136683989, -0.02763726511278163 },
            { -0.005677455049018714, -1.0733550709997748, 7.161712618512636, 0.001135951406383357, -0.02713795928814718,
                0.18667629256386825 },
            { 0.010597441416485083, -2.1812502831716207E-4, 0.001135951406383357, 4.707394050192636E-4,
                -6.65630008890527E-6, 3.3455860526058346E-5 },
            { -3.7857001323432846E-5, 0.014886082136683989, -0.02713795928814718, -6.65630008890527E-6,
                5.717574258947971E-4, -6.964693425150633E-4 },
            { -4.6627424747658885E-5, -0.02763726511278163, 0.18667629256386825, 3.3455860526058346E-5,
                -6.964693425150633E-4, 0.005012970805755819 },
        };

        final SymmetricPositiveMatrix refCov = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat1);

        // Propagate
        final SymmetricPositiveMatrix satCovMatrixFromMonoMeas =
            basicOrbCovProv.getOrbitalCovariance(refDate).getCovarianceMatrix();

        // Check results
        Assert.assertTrue(
            matrixEquals(satCovMatrixFromMonoMeas, refCov, relativeTolerance,
                absoluteTolerance, true));

        // Also check native frame is returned correctly
        Assert.assertTrue(basicOrbCovProv.getNativeFrame(refDate).equals(FramesFactory.getGCRF()));

    }

    /**
     * @throws PatriusException,
     *         IOException
     * @testType UT
     *
     * @testedFeature The robustness of the class {@link BasicOrbitalCovarianceProvider}
     *
     * @description The BasicOrbitalCovarianceProvider object is constructed with different issues. First, a covariance
     *              without parameter descriptors is build. Second, a covariance with the orbital parameter descriptors
     *              in the wrong order and finally a covariance that has different parameter descriptors that the ones
     *              defined in the mapper of the propagator.
     *
     *              A fixed-state propagator is used to avoid numerical differences in future evolutions of the
     *              numerical propagator.
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
    public void testExtractMapperIndexErrors() throws PatriusException {

        // Initialize the numerical propagator
        initializeMapperForFixedPropagator(1);

        // Create default covariance matrix
        final Covariance covariance = createInitialCovariance(false);

        // Build BasicOrbitalCovarianceProvider without descriptors
        try {
            final Covariance newCov = new Covariance(covariance.getCovarianceMatrix());

            // Build basic orbital covariance provider
            final BasicOrbitalCovarianceProvider basicOrbCovProv =
                new BasicOrbitalCovarianceProvider(newCov, this.fixedStatePropagator, this.mapper.get(0));

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage =
                "The 0th descriptor (ParameterDescriptor[parameter_name: p0]) of the covariance is not an orbital parameter";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

        // Build BasicOrbitalCovarianceProvider with orbital descriptors in the wrong order
        try {
            final List<ParameterDescriptor> newParamDesc = new ArrayList<>();
            final List<ParameterDescriptor> oldParamDesc = covariance.getParameterDescriptors();

            newParamDesc.add(oldParamDesc.get(1));
            newParamDesc.add(oldParamDesc.get(0));
            newParamDesc.add(oldParamDesc.get(2));
            newParamDesc.add(oldParamDesc.get(3));
            newParamDesc.add(oldParamDesc.get(4));
            newParamDesc.add(oldParamDesc.get(5));
            newParamDesc.add(oldParamDesc.get(6));

            final Covariance newCov = new Covariance(covariance.getCovarianceMatrix(), newParamDesc);

            // Build basic orbital covariance provider
            final BasicOrbitalCovarianceProvider basicOrbCovProv =
                new BasicOrbitalCovarianceProvider(newCov, this.fixedStatePropagator, this.mapper.get(0));

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {
            final String expectedMessage =
                "The orbital parameters of the covariance are not in the correct order";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

        // Different descriptors in the covariance than in the mapper. All ok except the last
        try {
            final List<ParameterDescriptor> newParamDesc = new ArrayList<>();
            final List<ParameterDescriptor> oldParamDesc = covariance.getParameterDescriptors();

            for (int i = 0; i < oldParamDesc.size() - 1; i++) {
                newParamDesc.add(oldParamDesc.get(i));
            }

            // Add a different param descriptor that is not in the mapper
            // Parameter descriptors
            final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = new LinkedHashMap<>();

            fieldDescriptorsMap.put(StandardFieldDescriptors.FORCE_MODEL, SolarRadiationPressure.class);
            newParamDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

            final Covariance newCov = new Covariance(covariance.getCovarianceMatrix(), newParamDesc);

            // Build basic orbital covariance provider
            final BasicOrbitalCovarianceProvider basicOrbCovProv =
                new BasicOrbitalCovarianceProvider(newCov, this.fixedStatePropagator, this.mapper.get(0));

            // Fail if this code is reached
            Assert.fail();
        } catch (final Exception e) {

            final String expectedMessage =
                "The descriptor ParameterDescriptor[force_model: SolarRadiationPressure] appears in the covariance but not in the mapper";
            Assert.assertTrue(e.getMessage().equals(expectedMessage));
        }

    }

    /**
     * Creates the initial covariance. The initial covariance matrix comes from the value of the ficus test
     * CovarianceAnalysisTest#testExternalValidationCovarianceAnalysisPropagation, the covariance is obtained from
     * "covAnal.getLowLevelCovarianceAnalysis().getCovFromMeasNoiseMatrix()". Only the information related to the
     * orbital coordinates or to the forces in the propagator is copied.
     *
     * @param isOnlyOrbital
     *        true if only orbital elements are to be added to the matrix
     *
     * @return
     */
    private Covariance createInitialCovariance(final boolean isOnlyOrbital) {

        // Complete matrix
        RealMatrix covMatrix =
            new Array2DRowRealMatrix(new double[][] {
                { 0.3380244634377018, -0.0020900807536444205, 0.013011279637499502, -0.010587642726467,
                    2.6003762344860027E-4, -0.001541747765774308, -718.989620996687 },
                { -0.0020900807536444205, 0.32818145998332154, -0.01903754331915291, 8.118643133508654E-5,
                    -0.010845793809882133, 0.0037256576583996046, 2132.4218617625115 },
                { 0.013011279637499502, -0.01903754331915291, 0.5135381316994596, -3.68047064198544E-4,
                    0.004224963460930337, -0.03904613773768623, -15557.667812626263 },
                { -0.010587642726467, 8.118643133508654E-5, -3.68047064198544E-4, 4.7073531090617685E-4,
                    -6.612536468165723E-6, 3.3178490668834725E-5, 12.462331311762782 },
                { 2.6003762344860027E-4, -0.010845793809882133, 0.004224963460930337, -6.612536468165723E-6,
                    5.719528461627855E-4, -6.976013815781905E-4, -383.5595392900952 },
                { -0.001541747765774308, 0.0037256576583996046, -0.03904613773768623, 3.3178490668834725E-5,
                    -6.976013815781905E-4, 0.005020109437012331, 2478.046541302513 },
                { -718.989620996687, 2132.4218617625115, -15557.667812626263, 12.462331311762782, -383.5595392900952,
                    2478.046541302513, 1.3486031100078409E9 },
            });

        // Limit to orbital elements
        if (isOnlyOrbital) {
            covMatrix = covMatrix.getSubMatrix(0, 5, 0, 5);
        }

        // Parameter descriptors
        final List<ParameterDescriptor> paramDesc = new ArrayList<>();
        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = new LinkedHashMap<>();

        // Orbital parameter descriptors
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.X);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        fieldDescriptorsMap.clear();
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.Y);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        fieldDescriptorsMap.clear();
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.Z);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        fieldDescriptorsMap.clear();
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VX);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        fieldDescriptorsMap.clear();
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VY);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        fieldDescriptorsMap.clear();
        fieldDescriptorsMap.put(StandardFieldDescriptors.ORBITAL_COORDINATE, CartesianCoordinate.VZ);
        paramDesc.add(new ParameterDescriptor(fieldDescriptorsMap));

        if (!isOnlyOrbital) {
            // Drag parameter descriptor from the propagator
            for (final Parameter param : this.mapper.get(0).getParametersList()) {
                if (param.getName().contains("DragForce")) {
                    paramDesc.add(param.getDescriptor());
                }
            }
        }

        // Build covariance object
        return new Covariance(new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, covMatrix), paramDesc);
    }

    /**
     * References obtained from the ficus test
     * CovarianceAnalysisTest#testExternalValidationCovarianceAnalysisPropagation
     *
     * @return
     */
    private Map<AbsoluteDate, SymmetricPositiveMatrix> getReferences() {

        // References map
        final Map<AbsoluteDate, SymmetricPositiveMatrix> refs = new LinkedHashMap<>();

        // 1st ref
        final AbsoluteDate date1 = new AbsoluteDate(-33, 0.8160000000000025);
        final double[][] mat1 = {
            { 0.33802446343770176, -0.002090080753644421, 0.013011279637499505, -0.010587642726467,
                2.6003762344860016E-4, -0.0015417477657743072, -718.9896209966873 },
            { -0.002090080753644421, 0.32818145998332154, -0.01903754331915291, 8.118643133508654E-5,
                -0.010845793809882131, 0.0037256576583996003, 2132.4218617625115 },
            { 0.013011279637499505, -0.01903754331915291, 0.5135381316994597, -3.680470641985441E-4,
                0.004224963460930332, -0.039046137737686205, -15557.667812626263 },
            { -0.010587642726467, 8.118643133508654E-5, -3.680470641985441E-4, 4.70735310906177E-4,
                -6.612536468165722E-6, 3.317849066883472E-5, 12.46233131176279 },
            { 2.6003762344860016E-4, -0.010845793809882131, 0.004224963460930332, -6.612536468165722E-6,
                5.719528461627853E-4, -6.976013815781889E-4, -383.55953929009473 },
            { -0.0015417477657743072, 0.0037256576583996003, -0.039046137737686205, 3.317849066883472E-5,
                -6.976013815781889E-4, 0.005020109437012321, 2478.046541302511 },
            { -718.9896209966873, 2132.4218617625115, -15557.667812626263, 12.46233131176279, -383.55953929009473,
                2478.046541302511, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov1 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat1);
        refs.put(date1, cov1);

        // 2nd ref
        final AbsoluteDate date2 = new AbsoluteDate(-28, 0.8160000000000025);
        final double[][] mat2 = {
            { 0.24393001755755395, -6.590292394250583E-4, 0.004973926975012848, -0.00823181416872713,
                1.8272299295808125E-4, -0.001100183147552477, -655.6560359305055 },
            { -6.590292394250583E-4, 0.23412518702274948, 0.00228434130895714, 5.0500534262045284E-5,
                -0.00802201547849076, 4.6899373062032164E-4, 440.6967831886632 },
            { 0.004973926975012848, 0.00228434130895714, 0.2566559924282683, -2.181867586593441E-4,
                8.305766700449338E-4, -0.014623230719576902, -4575.4103111009435 },
            { -0.00823181416872713, 5.0500534262045284E-5, -2.181867586593441E-4, 4.7055006682406367E-4,
                -5.972700337236671E-6, 2.9253665442454197E-5, 13.36442595277255 },
            { 1.8272299295808125E-4, -0.00802201547849076, 8.305766700449338E-4, -5.972700337236671E-6,
                5.266751288385157E-4, -4.090171598702142E-4, -293.12854106897777 },
            { -0.001100183147552477, 4.6899373062032164E-4, -0.014623230719576902, 2.9253665442454197E-5,
                -4.090171598702142E-4, 0.003185653845811732, 1914.8439075825872 },
            { -655.6560359305055, 440.6967831886632, -4575.4103111009435, 13.36442595277255, -293.12854106897777,
                1914.8439075825872, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov2 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat2);
        refs.put(date2, cov2);

        // 3rd ref
        final AbsoluteDate date3 = new AbsoluteDate(-23, 0.8160000000000025);
        final double[][] mat3 = {
            { 0.17337870056201202, 2.540472352407718E-4, -2.4922786049759774E-4, -0.005878820715383631,
                1.1210423668042211E-4, -6.99535267468055E-4, -580.4105461693405 },
            { 2.540472352407718E-4, 0.16676116486853826, 2.2887013423037545E-4, 2.0584872111843996E-5,
                -0.005492230886111825, -9.214246836863364E-4, -798.8481582044067 },
            { -2.4922786049759774E-4, 2.2887013423037545E-4, 0.18109122722997525, -7.555284660649781E-5,
                -6.676545197664789E-4, -0.002194932588002258, 3590.671405331654 },
            { -0.005878820715383631, 2.0584872111843996E-5, -7.555284660649781E-5, 4.704858706191526E-4,
                -5.656707468077149E-6, 2.7541312070567927E-5, 17.22738828023766 },
            { 1.1210423668042211E-4, -0.005492230886111825, -6.676545197664789E-4, -5.656707468077149E-6,
                4.93497479077011E-4, -1.9594651298136096E-4, -202.68830856683584 },
            { -6.99535267468055E-4, -9.214246836863364E-4, -0.002194932588002258, 2.7541312070567927E-5,
                -1.9594651298136096E-4, 0.001821461545219785, 1351.581360094986 },
            { -580.4105461693405, -798.8481582044067, 3590.671405331654, 17.22738828023766, -202.68830856683584,
                1351.581360094986, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov3 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat3);
        refs.put(date3, cov3);

        // 4th ref
        final AbsoluteDate date4 = new AbsoluteDate(-18, 0.8160000000000025);
        final double[][] mat4 = {
            { 0.12634864829326228, 6.745704018590641E-4, -0.0028149586355618386, -0.003527294162897403,
                4.935120618186933E-5, -3.4610969078236484E-4, -478.444924350947 },
            { 6.745704018590641E-4, 0.12360292282001223, -0.009223857766624004, -1.28405293045174E-5,
                -0.0031657391409969852, -0.0010120303412552132, -1586.179156618251 },
            { -0.0028149586355618386, -0.009223857766624004, 0.18453308233616583, 8.94402189471927E-5,
                -8.361569252091296E-4, 0.0017661340490355026, 8940.354781324959 },
            { -0.003527294162897403, -1.28405293045174E-5, 8.94402189471927E-5, 4.7059365076110743E-4,
                -5.0691188730768314E-6, 2.4333145806826288E-5, 24.052666369334457 },
            { 4.935120618186933E-5, -0.0031657391409969852, -8.361569252091296E-4, -5.0691188730768314E-6,
                4.724244280725142E-4, -5.8418524585003985E-5, -112.24378864087315 },
            { -3.4610969078236484E-4, -0.0010120303412552132, 0.0017661340490355026, 2.4333145806826288E-5,
                -5.8418524585003985E-5, 9.277186999775736E-4, 788.2897080746494 },
            { -478.444924350947, -1586.179156618251, 8940.354781324959, 24.052666369334457, -112.24378864087315,
                788.2897080746494, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov4 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat4);
        refs.put(date4, cov4);

        // 5th ref
        final AbsoluteDate date5 = new AbsoluteDate(-13, 0.8160000000000025);
        final double[][] mat5 = {
            { 0.10283434555170604, 6.372201984318534E-4, -0.0029181407504399004, -0.0011752787043407785,
                -3.963835677763543E-7, -7.093609547139029E-5, -334.94531543092523 },
            { 6.372201984318534E-4, 0.103071184312077, -0.01575923771475657, -4.810063970960963E-5,
                -9.518064486051432E-4, -3.694678502702762E-4, -1921.287148263552 },
            { -0.0029181407504399004, -0.01575923771475657, 0.19995228176849245, 2.692928344784864E-4,
                -2.4157249142085088E-4, 7.887265842927947E-4, 11473.571355969234 },
            { -0.0011752787043407785, -4.810063970960963E-5, 2.692928344784864E-4, 4.7096338967841743E-4,
                -3.6141185958969576E-6, 1.5918459980471453E-5, 33.841063174886116 },
            { -3.963835677763543E-7, -9.518064486051432E-4, -2.4157249142085088E-4, -3.6141185958969576E-6,
                4.634578561470116E-4, 3.5542477881917076E-6, -21.799930938541998 },
            { -7.093609547139029E-5, -3.694678502702762E-4, 7.887265842927947E-4, 1.5918459980471453E-5,
                3.5542477881917076E-6, 5.04508554526381E-4, 224.99977819914582 },
            { -334.94531543092523, -1921.287148263552, 11473.571355969234, 33.841063174886116, -21.799930938541998,
                224.99977819914582, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov5 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat5);
        refs.put(date5, cov5);

        // 6th ref
        final AbsoluteDate date6 = new AbsoluteDate(-8, 0.8160000000000025);
        final double[][] mat6 = {
            { 0.1028541287696246, 2.3557383910791086E-4, -0.0011008938054950861, 0.0011800943928377337,
                -2.8028285767509933E-5, 7.022708856865511E-5, -135.09546279684344 },
            { 2.3557383910791086E-4, 0.10449407167534298, -0.014729519569800344, -7.75612748095457E-5,
                0.0012403090311941865, 4.395613076057738E-4, -1804.1878208396038 },
            { -0.0011008938054950861, -0.014729519569800344, 0.1956100024160234, 4.1939307465654055E-4,
                5.493966799904489E-4, -0.0015979827224945756, 11190.406822154322 },
            { 0.0011800943928377337, -7.75612748095457E-5, 4.1939307465654055E-4, 4.717241263796049E-4,
                -6.958387686138573E-7, -1.4138495775479656E-6, 46.59273635537185 },
            { -2.8028285767509933E-5, 0.0012403090311941865, 5.493966799904489E-4, -6.958387686138573E-7,
                4.665969909290168E-4, -1.002421567369255E-5, 68.63831389515983 },
            { 7.022708856865511E-5, 4.395613076057738E-4, -0.0015979827224945756, -1.4138495775479656E-6,
                -1.002421567369255E-5, 5.518113594926789E-4, -338.25759657645307 },
            { -135.09546279684344, -1804.1878208396038, 11190.406822154322, 46.59273635537185, 68.63831389515983,
                -338.25759657645307, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov6 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat6);
        refs.put(date6, cov6);

        // 7th ref
        final AbsoluteDate date7 = new AbsoluteDate(-3, 0.8160000000000025);
        final double[][] mat7 = {
            { 0.1264609410741796, -3.282471401108531E-4, 0.0014382171350733764, 0.0035429318763053343,
                -2.0465438323558166E-5, -3.0971061715397277E-6, 135.92006516562932 },
            { -3.282471401108531E-4, 0.1281070622483303, -0.007153617632595621, -8.762900746571773E-5,
                0.003501332315101251, 8.484538304226509E-4, -1234.921614006691 },
            { 0.0014382171350733764, -0.007153617632595621, 0.17505719463853647, 4.580148106931264E-4,
                9.701433811122877E-4, -0.0018653723752730896, 8091.101029834479 },
            { 0.0035429318763053343, -8.762900746571773E-5, 4.580148106931264E-4, 4.730439165495834E-4,
                4.2813152824064786E-6, -3.137325977486271E-5, 62.307198259955996 },
            { -2.0465438323558166E-5, 0.003501332315101251, 9.701433811122877E-4, 4.2813152824064786E-6,
                4.818384073381931E-4, -9.91333967363771E-5, 159.06599601071048 },
            { -3.0971061715397277E-6, 8.484538304226509E-4, -0.0018653723752730896, -3.137325977486271E-5,
                -9.91333967363771E-5, 0.001069504368229142, -901.4515881866895 },
            { 135.92006516562932, -1234.921614006691, 8091.101029834479, 62.307198259955996, 159.06599601071048,
                -901.4515881866895, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov7 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat7);
        refs.put(date7, cov7);

        // 8th ref
        final AbsoluteDate date8 = new AbsoluteDate(2, 0.8160000000000025);
        final double[][] mat8 = {
            { 0.17375633542821414, -6.939540862420818E-4, 0.002535122288693327, 0.005918902968793639,
                3.933593596036531E-5, -3.9607912498457307E-4, 492.9126488214834 },
            { -6.939540862420818E-4, 0.17505269582396923, 2.846298318635341E-4, -5.875513937296512E-5,
                0.005921946735562133, 2.908595996935588E-4, -213.55371094513717 },
            { 0.002535122288693327, 2.846298318635341E-4, 0.17712424732077572, 2.6634111759263346E-4,
                4.543103014429076E-4, 0.003513663007168348, 2176.0479337811166 },
            { 0.005918902968793639, -5.875513937296512E-5, 2.6634111759263346E-4, 4.751297501314131E-4,
                1.1912340124266141E-5, -7.766559667373495E-5, 80.98331607980438 },
            { 3.933593596036531E-5, 0.005921946735562133, 4.543103014429076E-4, 1.1912340124266141E-5,
                5.091760293758256E-4, -2.6373625004070476E-4, 249.47816814703475 },
            { -3.9607912498457307E-4, 2.908595996935588E-4, 0.003513663007168348, -7.766559667373495E-5,
                -2.6373625004070476E-4, 0.0020573619034134857, -1464.5513846225176 },
            { 492.9126488214834, -213.55371094513717, 2176.0479337811166, 80.98331607980438, 249.47816814703475,
                -1464.5513846225176, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov8 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat8);
        refs.put(date8, cov8);

        // 9th ref
        final AbsoluteDate date9 = new AbsoluteDate(7, 0.8160000000000025);
        final double[][] mat9 = {
            { 0.2449077175706859, -2.9352489124760044E-4, -0.0012484695060527439, 0.00831556280206596,
                1.7237784904853963E-4, -0.001238543007238511, 950.6863921863707 },
            { -2.9352489124760044E-4, 0.24738003361200997, -0.004760249649049956, 3.455626988941316E-5,
                0.008592769108119484, -0.0017991634897419888, 1259.8259789931412 },
            { -0.0012484695060527439, -0.004760249649049956, 0.27590100404008633, -3.1148704861883676E-4,
                -0.0015640551392911138, 0.018063748394609662, -6554.204514141329 },
            { 0.00831556280206596, 3.455626988941316E-5, -3.1148704861883676E-4, 4.7822742644544695E-4,
                2.2791309741510404E-5, -1.439910145937282E-4, 102.61931216351955 },
            { 1.7237784904853963E-4, 0.008592769108119484, -0.0015640551392911138, 2.2791309741510404E-5,
                5.486011337200488E-4, -5.037792252660023E-4, 339.86988742410233 },
            { -0.001238543007238511, -0.0017991634897419888, 0.018063748394609662, -1.439910145937282E-4,
                -5.037792252660023E-4, 0.003515055493663776, -2027.5262010957695 },
            { 950.6863921863707, 1259.8259789931412, -6554.204514141329, 102.61931216351955, 339.86988742410233,
                -2027.5262010957695, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov9 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat9);
        refs.put(date9, cov9);

        // 10th ref
        final AbsoluteDate date10 = new AbsoluteDate(12, 0.8160000000000025);
        final double[][] mat10 = {
            { 0.34016881651790215, 0.0016982737288475664, -0.014933244555376082, 0.010742674429240805,
                4.036116929380737E-4, -0.0026849117143558586, 1524.0348998941856 },
            { 0.0016982737288475664, 0.34804386909904345, -0.04029052606594774, 2.2373632250157258E-4,
                0.011604324946978068, -0.0059869949985583626, 3185.1028467871483 },
            { -0.014933244555376082, -0.04029052606594774, 0.5807071447682912, -0.0014682969408843299,
                -0.0056503462540473184, 0.04530606588074869, -18098.95452971257 },
            { 0.010742674429240805, 2.2373632250157258E-4, -0.0014682969408843299, 4.8262138693908247E-4,
                3.7511051737675356E-5, -2.340419794043779E-4, 127.21276449382664 },
            { 4.036116929380737E-4, 0.011604324946978068, -0.0056503462540473184, 3.7511051737675356E-5,
                6.001023551236715E-4, -8.191923005221356E-4, 430.23621713485886 },
            { -0.0026849117143558586, -0.0059869949985583626, 0.04530606588074869, -2.340419794043779E-4,
                -8.191923005221356E-4, 0.00544215408008772, -2590.3452911994973 },
            { 1524.0348998941856, 3185.1028467871483, -18098.95452971257, 127.21276449382664, 430.23621713485886,
                -2590.3452911994973, 1.3486031100078409E9 },
        };

        final SymmetricPositiveMatrix cov10 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.UPPER, mat10);
        refs.put(date10, cov10);

        return refs;

    }

    @Before
    public void setUpTest() throws PatriusException {

        // initialize class variables
        this.ephemeris = new ArrayList<>();
        this.mapper = new ArrayList<>();

        // Uncomment this part of code if we want to recompute the fixed ephemeris for the scStates
        // Create fixed spacecraft states so no propagation is required
        // final PVCoordinates satPVCoords =
        // new PVCoordinates(7179992.82, 2276.519, -14178.396, 7.450848, -1181.198684, 7356.62864);
        // try {
        // // Initialize the numerical propagator
        // buildNumericalPropagator(satPVCoords);
        // for (final AbsoluteDate date : getReferences().keySet()) {
        //
        // PVCoordinates pvCoord = this.ephemeris.get(0).getSpacecraftState(date).getPVCoordinates();
        //
        // }
        // } catch (final Exception e) {
        // // DUMMY
        // }

        final List<AbsoluteDate> dates = new ArrayList<>();
        dates.addAll(getReferences().keySet());

        // Coords
        // Initialize fixed pvCoords
        final List<PVCoordinates> pvCoords = new ArrayList<>();
        pvCoords.add(new PVCoordinates(7179992.819999999, 2276.518999999997, -14178.395999999966,
            7.450848000000014, -1181.198684, 7356.62864));
        pvCoords.add(new PVCoordinates(7179933.424833024, -3629.4785629842227, 22604.77300796113,
            -31.208961784113164, -1181.195041149309, 7356.605953955106));
        pvCoords.add(new PVCoordinates(7179680.730779564, -9535.37841052, 59387.33344396074,
            -69.86853312278362, -1181.1595977433142, 7356.38521073393));
        pvCoords.add(new PVCoordinates(7179234.741633958, -15441.021539384732, 96168.29501933922,
            -108.5268251631426, -1181.0923537462702, 7355.966410113791));
        pvCoords.add(new PVCoordinates(7178595.466394901, -21346.248948317647, 132946.6674576549,
            -147.18279702196193, -1180.993309978703, 7355.34955720496));
        pvCoords.add(new PVCoordinates(7177762.919265518, -27250.90164230147, 169721.46052135102,
            -185.83540781368862, -1180.8624681174954, 7354.534662451179));
        pvCoords.add(new PVCoordinates(7176737.119653311, -33154.82063684443, 206491.68403842463,
            -224.4836166784843, -1180.6998306959476, 7353.521741630056));
        pvCoords.add(new PVCoordinates(7175518.092169959, -39057.84696226236, 243256.34792909565,
            -263.12638281027137, -1180.5054011038164, 7352.310815853292));
        pvCoords.add(new PVCoordinates(7174105.866630974, -44959.82166796102, 280014.46223247785,
            -301.762665484785, -1180.279183587329, 7350.901911566783));
        pvCoords.add(new PVCoordinates(7172500.478055231, -50860.585826718496, 316765.0371332502,
            -340.3914240876302, -1180.0211832491775, 7349.295060550571));

        // Additional states
        // Initialize additional states
        final List<double[]> addStates = new ArrayList<>();
        addStates.add(new double[] { 1.0, 1.7709419926877477E-22, -1.8994134815748898E-21, 2.7755575615628914E-16,
            1.9684407870083002E-17, -1.2260168438498328E-16, 4.867699410933145E-22, 1.0, -1.7626711559144977E-20,
            -1.7202237245911408E-17, 2.7755575615628914E-16, 4.539666091779481E-19, -2.524141639205564E-21,
            -1.762666970534079E-20, 1.0000000000000002, 1.0713450400437846E-16, 4.539602195813207E-19,
            6.661338147750939E-16, 1.7999450129153882E-20, -1.5819829215076654E-23, 1.348304398461435E-22,
            1.0000000000000002, 7.062571467554319E-22, -4.200526548120844E-21, 5.624828165360588E-23,
            -1.0905549195899117E-20, -8.487890904688416E-25, -8.094996269597067E-22, 1.0, -7.79763029199083E-21,
            -3.221868485894043E-22, -7.415544944567182E-25, -1.0587911840678754E-21, 4.324603640003798E-21,
            -7.797676942069322E-21, 0.9999999999999999, -6.928930099838721E-23, 2.949932859517235E-22,
            -7.059986528140091E-22, 6.753929701495471E-22, 5.1864224407074835E-21, -7.934730025914918E-22,
            8.149021503354437E-24, 2.274746684520826E-23, 8.35452418678558E-23, 1.9521462456251453E-22,
            -4.764560328305439E-22, 9.512577044359818E-23 });
        addStates.add(new double[] { 1.0000269219385431, 1.7314627676404531E-9, -1.078378571634583E-8,
            5.000044869780806, -6.341362419805317E-9, 3.949453064230226E-8, 1.731219687460002E-9, 0.9999865390912424,
            -9.916372346767781E-12, -6.341693232340983E-9, 4.999977564922777, 1.3733357174956894E-11,
            -1.0782475158568094E-8, -9.91637938728364E-12, 0.9999865391514103, 3.9496252024979365E-8,
            1.3733436692114966E-11, 4.999977564902204, 1.076883506948358E-5, -1.52189688608765E-9, 9.478510608380246E-9,
            1.0000269219376694, -9.34108478535684E-9, 5.817715641923544E-8, -1.5220477754566118E-9,
            -5.384364974682742E-6, -6.5655254470258075E-12, -9.341327868014118E-9, 0.9999865389630124,
            -1.0257213157518709E-11, 9.479369014205601E-9, -6.565506363206659E-12, -5.384325138224674E-6,
            5.817846699137659E-8, -1.0257110696323217E-11, 0.9999865390503533, 7.715050837581864E-10,
            -7.560775962759814E-8, 1.676495601133492E-7, 3.788864289075921E-7, -1.0441389515939211E-6,
            1.6426545997038394E-7, 6.744050677927446E-10, -3.024326624664211E-8, 6.705999350274465E-8,
            1.5155422109848067E-7, -4.17656636056724E-7, 6.570603895862707E-8 });
        addStates.add(new double[] { 1.0001076887055909, -3.736340072226072E-8, 2.3270293811039148E-7,
            10.000358957055928, -2.721803849308505E-7, 1.6951664951273779E-6, -3.736687704433374E-8, 0.9999461564745539,
            -2.0509344046551045E-10, -2.721872056259588E-7, 9.999820519502304, -1.1841878316894817E-9,
            2.3272377551637131E-7, -2.0509303063202448E-10, 0.9999461577189641, 1.6952062635153E-6,
            -1.1841850055121706E-9, 9.999820526938414, 2.1537883072671926E-5, -1.633039326894128E-8,
            1.0170734911275944E-7, 1.0001076861446336, -1.2594346721928363E-7, 7.843886269724716E-7,
            -1.6331947046912883E-8, -1.0768655368799962E-5, -9.679379832037495E-11, -1.2594694359477488E-7,
            0.999946155884417, -7.375549196224522E-10, 1.0171686350531289E-7, -9.679362875634899E-11,
            -1.0768068069458964E-5, 7.844094647246127E-7, -7.375536337630597E-10, 0.9999461604097653,
            1.0401956006311403E-8, -3.0243561477240884E-7, 6.705985757751592E-7, 1.5155353093965736E-6,
            -4.176557895209791E-6, 6.570574442931962E-7, 3.5435373988712706E-9, -6.048830214966756E-8,
            1.3411917147593802E-7, 3.031043014377846E-7, -8.353081930348766E-7, 1.3141030951723034E-7 });
        addStates
            .add(new double[] { 1.0002423006407606, -1.8371403894055696E-7, 1.144189707392036E-6, 15.001211451919055,
                -1.6659718126728402E-6, 1.0375841462717899E-5, -1.837371528607947E-7, 0.9998788529398936,
                -1.344179073507747E-9, -1.6660330532687629E-6, 14.999394254733609, -1.2674108803951375E-8,
                1.1443318329460507E-6, -1.3441771659510529E-9, 0.9998788610957443, 1.0376213724059687E-5,
                -1.2674091874387649E-8, 14.999394332199026, 3.230685396431035E-5, -4.4423771444433606E-8,
                2.76675817494526E-7, 1.0002422824284956, -4.826619361708097E-7, 3.0060678981805977E-6,
                -4.443119947952968E-8, -1.615270435930148E-5, -4.068161283047502E-10, -4.82685050584885E-7,
                0.9998788514209084, -4.720293065954504E-9, 2.767218360269346E-7, -4.068156768871434E-10,
                -1.615023598823596E-5, 3.0062100269301123E-6, -4.720288201836511E-9, 0.9998788801367686,
                3.986397670509263E-8, -6.804963295057743E-7, 1.5088338090628486E-6, 3.409915754578604E-6,
                -9.397174384307472E-6, 1.4783625772838215E-6, 8.60691237856512E-9, -9.073666533159307E-8,
                2.0117387002924276E-7, 4.54646168744725E-7, -1.252931851843172E-6, 1.9711099217328663E-7 });
        addStates.add(new double[] { 1.000430756633021, -5.037368404405371E-7, 3.1373247196203945E-6,
            20.002871468133055, -5.7203527958109615E-6, 3.5626942319751235E-5, -5.038308723706633E-7,
            0.9997846301113386, -4.866427254113219E-9, -5.720674437841337E-6, 19.998564167527153, -6.133327582941437E-8,
            3.137907106362708E-6, -4.866422111225746E-9, 0.9997846596385573, 3.562892384845899E-5,
            -6.133321818129592E-8, 19.99856454067321, 4.30754575201086E-5, -8.579852517384256E-8, 5.343620785549974E-7,
            1.0004306904494462, -1.2123098998429977E-6, 7.550390733821914E-6, -8.582166413113557E-8,
            -2.153634512339127E-5, -1.0727405925680648E-9, -1.2124039344003507E-6, 0.9997846272819578,
            -1.6538772896868005E-8, 5.345058650114949E-7, -1.0727397279709772E-9, -2.1529836230306788E-5,
            7.550973137386815E-6, -1.6538760746055654E-8, 0.9997847277319947, 1.001265731983731E-7,
            -1.2098103250486389E-6, 2.6823237070038945E-6, 6.061976530605279E-6, -1.6705791904304647E-5,
            2.628158296242802E-6, 1.5863567477975818E-8, -1.209898609175291E-7, 2.682204272546198E-7,
            6.061757564981784E-7, -1.6705048057362668E-6, 2.6280623180949715E-7 });
        addStates.add(new double[] { 1.000673054119643, -1.0638261917102322E-6, 6.625619141239034E-6,
            25.005607992395575, -1.4631864226107554E-5, 9.112875372655045E-5, -1.0641094881636567E-6, 0.999663490447064,
            -1.2891536299775123E-8, -1.4633061133642916E-5, 24.99719566597267, -2.020441711004225E-7,
            6.627378456939001E-6, -1.2891525527489445E-8, 0.999663568666933, 9.113616582834483E-5,
            -2.0204402429370247E-7, 24.997196893452244, 5.38434032743201E-5, -1.40449359929872E-7, 8.747331574476649E-7,
            1.0006728797215307, -2.4476307680611235E-6, 1.524409694083694E-5, -1.405055570539316E-7,
            -2.6919410845374893E-5, -2.23063370860928E-9, -2.447914074355946E-6, 0.9996634862297943,
            -4.281467599503323E-8, 8.750827522896548E-7, -2.2306322995787815E-9, -2.6905876390688628E-5,
            1.5245856319072035E-5, -4.281465153613517E-8, 0.9996637461349932, 2.0215274447486466E-7,
            -1.890405415612502E-6, 4.191018414798763E-6, 9.47164608527421E-6, -2.6102099954931178E-5,
            4.106412676171368E-6, 2.5312062459121284E-8, -1.5124934087050335E-7, 3.3525518455473215E-7,
            7.57689003866637E-7, -2.0880042685882106E-6, 3.2849413881483715E-7 });
        addStates.add(new double[] { 1.0009691890848336, -1.9303455367894313E-6, 1.2022391426004878E-5,
            30.009689833614168, -3.1260530126971254E-5, 1.9469380251656916E-4, -1.9310464244543926E-6,
            0.9995154372392923, -2.8219394454476897E-8, -3.126406296092135E-5, 29.99515417460275, -5.278994552651198E-7,
            1.202674930650811E-5, -2.821937500268718E-8, 0.9995156084615285, 1.9471573217785376E-4,
            -5.278991424722055E-7, 29.995157379912165, 6.461040045734943E-5, -2.0836919334401848E-7,
            1.2977449444644306E-6, 1.000968809615119, -4.321269339683119E-6, 2.691331216713499E-5,
            -2.0848545075160468E-7, -3.2301734723506916E-5, -4.016502109134322E-9, -4.321970256480628E-6,
            0.9995154320793119, -9.221021292206189E-8, 1.2984685197091239E-6, -4.016500024512942E-9,
            -3.227736444170069E-5, 2.691767023181855E-5, -9.221016982162296E-8, 0.9995159917189741,
            3.5689709471909325E-7, -2.7223165409786886E-6, 6.034849795732147E-6, 1.363883257631323E-5,
            -3.758567417423778E-5, 5.9130842601606324E-6, 3.695047970072425E-8, -1.8151650334502672E-7,
            4.022744879709499E-7, 9.091818556440985E-7, -2.5054074831633943E-6, 3.9417279021619595E-7 });
        addStates.add(new double[] { 1.0013191560580625, -3.1696184395648987E-6, 1.97407116500301E-5, 35.01538557219633,
            -5.9129291834576655E-5, 3.682633337191909E-4, -3.1711288577068536E-6, 0.999340474614205,
            -5.4329735620078545E-8, -5.9138147545485434E-5, 34.99230513965579, -1.1804017474603116E-6,
            1.9750108727478734E-5, -5.43297037831839E-8, 0.9993408042619529, 3.6831837178912544E-4,
            -1.1804011569085253E-6, 34.992312304864186, 7.537615793339937E-5, -2.8954915585794977E-7,
            1.803342199092452E-6, 1.0013184293605975, -6.965743198822867E-6, 4.33833697542881E-5,
            -2.8976427349236666E-7, -3.768314997683878E-5, -6.56627422127966E-9, -6.967253690111446E-6,
            0.9993404696979057, -1.754269429162772E-7, 1.8046814048562052E-6, -6.566271330030896E-9,
            -3.764330885097418E-5, 4.339276729259539E-5, -1.754268735299413E-7, 0.9993415342834815, 5.75303447454521E-7,
            -3.705585495856888E-6, 8.213731457640965E-6, 1.856342389934858E-5, -5.115597650344153E-5,
            8.048121895394764E-6, 5.077642464623763E-8, -2.117926920592544E-7, 4.69274689510688E-7,
            1.0606502621843144E-6, -2.922691729383346E-6, 4.5984023082027476E-7 });
        addStates.add(new double[] { 1.0017229481120764, -4.847919649865761E-6, 3.0193345870492322E-5,
            40.02296350936129, -1.0242330004455353E-4, 6.379029013544562E-4, -4.850859493470581E-6, 0.9991386075318225,
            -9.538170303274397E-8, -1.0244296821956855E-4, 39.98851403433791, -2.35965672581827E-6,
            3.021164250915652E-5, -9.538165445031353E-8, 0.9991391862639978, 6.380252233381457E-4,
            -2.3596557042388115E-6, 39.98852835566371, 8.614038413816919E-5, -3.839785915766381E-7,
            2.3914585553275404E-6, 1.0017216780532663, -1.051341412366361E-5, 6.547863266863746E-5,
            -3.843453084774586E-7, -4.3063489852066446E-5, -1.0015781952019782E-8, -1.0516354129909723E-5,
            0.9991386050052343, -3.0520426324187535E-7, 2.3937418558771593E-6, -1.0015778123252623E-8,
            -4.300271869942443E-5, 6.549693032955902E-5, -3.05204158615821E-7, 0.9991404570449907, 8.683024622356033E-7,
            -4.8402606561701855E-6, 1.072755878603635E-5, 2.4245287715539682E-5, -6.681235539321301E-5,
            1.0511464574901572E-5, 6.678702637266628E-8, -2.420791956857126E-7, 5.36252148473853E-7,
            1.2120901793334443E-6, -3.339834332587981E-6, 5.254944743655433E-7 });
        addStates.add(new double[] { 1.002180556860599, -7.0314661743918635E-6, 4.379270051444441E-5,
            45.032691616479774, -1.6598906642191166E-4, 0.0010337970845729395, -7.036758338605229E-6,
            0.9989098417858477, -1.562133191417687E-7, -1.6602886035992637E-4, 44.98364636408357, -4.334557927984031E-6,
            4.382564419534362E-5, -1.5621324879933107E-7, 0.998910789616266, 0.0010340446784281032,
            -4.334556274068818E-6, 44.98367266926765, 9.69027870166153E-5, -4.916450593216116E-7, 3.0620165282339157E-6,
            1.0021784846594364, -1.5096459511710852E-5, 9.40223155347865E-5, -4.922321928649949E-7,
            -4.844258763037735E-5, -1.4500742381730485E-8, -1.5101752005223351E-5, 0.9989098449729101,
            -4.963175677831499E-7, 3.0656725253986056E-6, -1.4500737484723955E-8, -4.835460379917296E-5,
            9.405526128185704E-5, -4.963174176557985E-7, 0.9989128566254254, 1.246809254502589E-6,
            -6.126396702374616E-6, 1.3576208983962185E-5, 3.06842714788603E-5, -8.455404605191939E-5,
            1.3303041285183274E-5, 8.497893827923196E-8, -2.723772472606457E-7, 6.032032327781597E-7,
            1.363497568361151E-6, -3.756812671789808E-6, 5.911335046840126E-7 });

        final List<SpacecraftState> scStates = new ArrayList<>();

        for (int i = 0; i < getReferences().size(); i++) {
            // Create fixed scState
            final CartesianOrbit orbit = new CartesianOrbit(pvCoords.get(i), FramesFactory.getGCRF(),
                dates.get(i), Constants.EGM96_EARTH_MU);
            final SpacecraftState scState = new SpacecraftState(orbit);
            scStates.add(scState.addAdditionalState("PDE", addStates.get(i)));
        }

        this.fixedStatePropagator = new Ephemeris(scStates, 2);
        this.fixedStatePropagator.setOrbitFrame(FramesFactory.getGCRF());

    }

}
