package fr.cnes.sirius.patrius.covariance;

/** HISTORY
 * VERSION:4.16:OPENFD-379:25/04/2025:[PATRIUS] Ajout d'une implementation basique de OrbitalCovarianceProvider
 * END-HISTORY
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.PredefinedEphemerisType;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.JacobiansMapper;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public abstract class AbstractBasicOrbitalCovarianceProviderTest {

    /** Propagator-generated ephemeris. */
    protected List<BoundedPropagator> ephemeris;

    /** Jacobian mapper of the partial derivatives, used in case of numerical propagator. */
    protected List<JacobiansMapper> mapper;

    @BeforeClass
    public static void setUp() throws PatriusException, IOException {

        Utils.clear();

        // Data location
        Utils.setDataRoot("regular-dataPBASE");

    }

    protected NumericalPropagator buildNumericalPropagator(final PVCoordinates satPVCoords, final int satIndex)
        throws PatriusException {
        return buildNumericalPropagator(satPVCoords, satIndex, OrbitType.CARTESIAN, PositionAngle.TRUE,
            FramesFactory.getGCRF());
    }

    /**
     * Private method to build the default Numerical {@link #propagator} and load the {@link #ephemeris} and
     * {@link #mapper} fields.<br>
     * In addition, this method adds a default drag force via the {@link #buildDefaultDragForce()} method if required.
     *
     * @return
     *
     * @throws PatriusException
     *         if any errors occurs at data loading
     */
    protected NumericalPropagator buildNumericalPropagator(final PVCoordinates satPVCoords, final int satIndex,
                                                           final OrbitType orbitType, final PositionAngle positionAngle,
                                                           final Frame orbitFrame)
        throws PatriusException {

        // Configuration
        final FirstOrderIntegrator integrator = buildIntegrator();
        final AbsoluteDate orbitDate = AbsoluteDate.J2000_EPOCH;

        final AttitudeProvider attProv = new LofOffset(FramesFactory.getGCRF(), LOFType.QSW);

        final CartesianOrbit orbit = new CartesianOrbit(satPVCoords, orbitFrame, orbitDate, Constants.EGM96_EARTH_MU);

        NumericalPropagator numPropagator;
        try {
            numPropagator = new NumericalPropagator(integrator, orbitFrame, orbitType, positionAngle);
        } catch (final PatriusException e) {
            throw new IllegalStateException(
                "Shouldn't happen as the propagation frame is check to be pseudo inertial before");
        }

        numPropagator.setAttitudeProvider(attProv);
        // Define Newtonian gravity model in the propagation frame to avoid useless frame conversions
        final NewtonianGravityModel newtonianGravityModel =
            new NewtonianGravityModel(orbitFrame, Constants.EGM96_EARTH_MU);
        final ForceModel newtonianAttraction = new DirectBodyAttraction(newtonianGravityModel);
        numPropagator.addForceModel(newtonianAttraction);

        final SpacecraftState initialState = new SpacecraftState(orbit);
        numPropagator.setInitialState(initialState);

        // Add drag force
        final DragForce dragForce = buildDefaultDragForce(satIndex);
        numPropagator.addForceModel(dragForce);

        // Add SRP
        Parameter prs = null;
        SolarRadiationPressure srp = null;
        final double srpCoeff = 1.0;

        final JPLCelestialBodyLoader loader = initJPLLoader();
        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());
        prs = new Parameter("kPRS_" + satIndex, 2.0);
        final RadiationSensitive radiativeModel =
            new DirectRadiativeModel(getSphericalSpacecraft(500, 1, srpCoeff), prs);

        srp = new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
            radiativeModel);

        numPropagator.addForceModel(srp);

        // Partial derivatives
        final PartialDerivativesEquations partials = new PartialDerivativesEquations("PDE", numPropagator);
        final Parameter[] selectedParameters = { dragForce.getParameters().get(0), prs };
        partials.selectParameters(selectedParameters);

        try {
            final SpacecraftState initialStateWithAdditionalStates = partials.setInitialJacobians(initialState);
            numPropagator.setInitialState(initialStateWithAdditionalStates);

            numPropagator.setEphemerisMode();
            numPropagator.propagate(orbitDate.shiftedBy(-3.0));
            numPropagator.propagate(orbitDate.shiftedBy(86370.0 + 33.0));
            this.ephemeris.add(numPropagator.getGeneratedEphemeris());
            this.mapper.add(partials.getMapper());

        } catch (final PatriusException e) {
            throw new IllegalStateException("The state cannot be propagated");
        }

        return numPropagator;
    }

    /**
     * Initialize mapper for Fixed Propagator tests
     * 
     * @param satPVCoords
     * @param satIndex
     * @throws PatriusException
     */
    public void initializeMapperForFixedPropagator(final int satIndex) throws PatriusException {

        // Add drag force parameter
        final Parameter dragForce = new Parameter("DragForce_" + satIndex, 1.0);

        // Add SRP
        final Parameter prs = new Parameter("kPRS_" + satIndex, 2.0);

        // Initialize mapper
        final List<Parameter> selectedParameters = new ArrayList<>();
        selectedParameters.add(dragForce);
        selectedParameters.add(prs);

        this.mapper.add(new JacobiansMapper("PDE", selectedParameters, OrbitType.CARTESIAN, PositionAngle.TRUE,
            FramesFactory.getGCRF()));

    }

    /**
     * Initializes the Celestial Body Factory.
     */
    private JPLCelestialBodyLoader initJPLLoader() throws PatriusException {
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader("unxp2000.405",
            PredefinedEphemerisType.SUN);
        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            PredefinedEphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            PredefinedEphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(
            CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        return loader;
    }

    /**
     * Private method to build the default drag force used by the Numerical {@link #propagator}.
     *
     * @param satIndex
     *
     * @return the default drag force model to be added to the propagator
     * @throws IllegalArgumentException
     *         if the {@link #dragCoefficient} has not been initialized
     */
    private DragForce buildDefaultDragForce(final int satIndex) {

        final double dragCoefficient = 1.0;

        final SimpleExponentialAtmosphere atm =
            new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Constants.CNES_STELA_AE,
                Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "main"), 1e-15, 150000, 1400000);
        final AssemblyBuilder builder = new AssemblyBuilder();
        // Add main part (one sphere) and part2 (one facet)
        final String main = "main";
        final String part1 = "part1";
        builder.addMainPart(main);
        builder.addPart(part1, main, Transform.IDENTITY);
        // One facet
        final Vector3D normal = new Vector3D(0., 0., -2.);
        final Facet facet = new Facet(normal, 25 * MathLib.PI);

        // Adding aero properties
        final IPartProperty aeroFacetProp = new AeroFacetProperty(facet);
        builder.addProperty(aeroFacetProp, part1);
        // sphere property
        final double radius = 10.;
        final IPartProperty aeroProp = new AeroSphereProperty(radius);
        builder.addProperty(aeroProp, main);
        builder.addProperty(aeroProp, part1);

        // Adding mass properties
        IPartProperty massMainProp;
        final IPartProperty part2Prop;
        try {
            massMainProp = new MassProperty(100.);
            part2Prop = new MassProperty(10.);
            builder.addProperty(massMainProp, main);
            builder.addProperty(part2Prop, part1);
        } catch (final PatriusException e) {
            e.printStackTrace();
        }

        // Assembly creation
        final Assembly assembly = builder.returnAssembly();
        final DragSensitive sp =
            new AeroModel(assembly, atm, new OneAxisEllipsoid(6378000, 0, FramesFactory.getGCRF()), 1);
        return new DragForce(new Parameter("coefK_" + satIndex, dragCoefficient), atm, sp);
    }

    /**
     * Returns a simple vehicle, with given properties for drag and srp dissipative forces. The vehicle has a spherical
     * shape.
     *
     * @param mass
     *        Spacecraft's mass
     * @param radius
     *        Spacecraft's radius
     * @param dragCx
     *        Spacecraft's drag coefficient
     * @return a valid Assembly
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public static Assembly getSphericalSpacecraft(final double mass, final double radius, final double dragCx)
        throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();
        final String body = "BODY";

        builder.addMainPart(body);

        // mass
        builder.addProperty(new MassProperty(mass), body);

        // shape for radiative force
        builder.addProperty(new RadiativeSphereProperty(radius), body);

        // thermo-optical coefficients (ka, ks, kd)
        // Solar radiation pressure absorption coefficient.
        final double PRS_KA = 0.5;

        // Solar radiation pressure specular coefficient.
        final double PRS_KS = 0.2;

        // Solar radiation pressure diffusion coefficient.
        final double PRS_KD = 0.3;

        builder.addProperty(new RadiativeProperty(PRS_KA, PRS_KS, PRS_KD), body);

        // drag coefficient
        builder.addProperty(new AeroSphereProperty(radius, dragCx), body);

        return builder.returnAssembly();
    }

    /**
     * Private method to build the default {@link #integrator} used by the Numerical {@link #propagator}.
     *
     * @return
     */
    private DormandPrince853Integrator buildIntegrator() {
        final double minStep = 0.0;
        final double maxStep = 60.;
        final double coeff = 1e-3;
        final double absTol = 1.e-6;

        final double[] vecAbsoluteTolerance =
            { absTol, absTol, absTol, absTol * coeff, absTol * coeff, absTol * coeff };
        final double[] vecRelativeTolerance = { 0., 0., 0., 0., 0., 0. };

        return new DormandPrince853Integrator(minStep, maxStep, vecAbsoluteTolerance, vecRelativeTolerance);
    }

    /**
     * Compare two matrices, with relative or absolute tolerance
     * ({@link Precision#equalsWithAbsoluteOrRelativeTolerances}.
     *
     * @param matrix
     *        First matrix
     * @param matrixRef
     *        Second reference matrix
     * @param relativeTolerance
     *        The relative tolerance
     * @param absoluteTolerance
     *        The absolute tolerance
     * @param verbose
     *        Indicates if the non-equal elements should be printed
     * @return the comparison matrix
     */
    public final static boolean matrixEquals(final RealMatrix matrix, final RealMatrix matrixRef,
                                             final double relativeTolerance,
                                             final double absoluteTolerance, final boolean verbose) {
        // Check the matrices are the same dimension
        final int rows = matrix.getRowDimension();
        if (rows != matrixRef.getRowDimension()) {
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, rows,
                matrixRef.getRowDimension());
        }
        final int cols = matrix.getColumnDimension();
        if (cols != matrixRef.getColumnDimension()) {
            throw new DimensionMismatchException(PatriusMessages.DIMENSIONS_MISMATCH_SIMPLE, cols,
                matrixRef.getColumnDimension());
        }

        // Fill the comparison array
        boolean isEqual = true;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (!Precision.equalsWithAbsoluteOrRelativeTolerances(matrix.getEntry(i, j), matrixRef.getEntry(i, j),
                    relativeTolerance,
                    absoluteTolerance)) {
                    if (verbose) {
                        isEqual = false;
                        final double value = matrix.getEntry(i, j);
                        final double valueRef = matrixRef.getEntry(i, j);
                        System.out.println(
                            String.format(Locale.US,
                                "The element (%d,%d) is not equal: value=%.16g, valueRef=%.16g, diff=%g, relativeDiff=%g",
                                i, j, value, valueRef, value - valueRef, (value - valueRef) / valueRef));
                    } else {
                        return false;
                    }
                }
            }
        }
        return isEqual;
    }

}
