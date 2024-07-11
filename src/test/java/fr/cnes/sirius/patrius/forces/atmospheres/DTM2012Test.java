/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2854:18/05/2021:Test de comparaison DTM2000 vs DTM 2012 à pérenniser 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test class for {@link DTM2012} class.
 */
public class DTM2012Test {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(DTM2012Test.class.getSimpleName(), "DTM2012 atmospherePATRIUS");
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }
    
    /**
     * Check that density and temperatures are returned as expected (reference: CNES with only 4 digits).
     */
    @Test
    public void testData() throws PatriusException, ParseException {

        Report.printMethodHeader("testData", "Density/temperature computation", "Sean Bruisma CNES", 1e-4,
            ComparisonType.RELATIVE);

        final Frame itrf = FramesFactory.getITRF();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        earth.setAngularThreshold(1e-10);
        AtmosphereData data;

        double roTestCase;
        double tzTestCase;
        double tinfTestCase;
        
        double day;
        double alti;
        double xlon;
        double lat;
        double hl;
        double f;
        double fbar;
        double akp3;
        double akp24;
        
        // Inputs :
        day=15;
        alti=300;	//km
        xlon=0;		//km
        lat=45;		//degrees
        hl =16;		//hours
        f=70;		//instantaneous flux
        fbar=70;	//mean flux on the last 24 hours
        akp3 = 1.;
        akp24 = 1.;
        
        // Target outputs  (CNES reference)
        roTestCase = 7.014E-15 * 1000;
        tinfTestCase = 666.911;
        tzTestCase = 666.860;

        // Computation and check
        data = this.computeData(earth, day, alti*1000, xlon*1000, MathLib.toRadians(lat), hl*FastMath.PI/12, f, fbar, akp3, akp24);
        Assert.assertEquals(0, (roTestCase - data.getDensity()) / roTestCase, 1e-4);
        Assert.assertEquals(0, (tzTestCase - data.getLocalTemperature()) / tzTestCase, 1e-6);
        Assert.assertEquals(0, (tinfTestCase - data.getExosphericTemperature()) / tinfTestCase, 1e-7);
        
        Report.printToReport("Density at day 15 (altitude: 300km)", roTestCase, data.getDensity());
        Report.printToReport("T_local at day 15 (altitude: 300km)", tzTestCase, data.getLocalTemperature());
        Report.printToReport("T_exotic at day 15 (altitude: 300km)", tinfTestCase, data.getExosphericTemperature());

    }

    
    /**
     * Convert old test cases and its input values to new format.
     */
    private final AtmosphereData computeData(final OneAxisEllipsoid earth, final double day,
                                             final double alti, final double lon, final double lat,
                                             final double hl, final double f, final double fbar,
                                             final double akp3, final double akp24) throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI()).shiftedBy(86400 * day);
        final Vector3D pos = earth.transform(new GeodeticPoint(lat, lon, alti));
        final DTMInputParameters params = new DTMInputParameters(){
            @Override
            public double getThreeHourlyKP(final AbsoluteDate date) throws PatriusException {
                return akp3;
            }

            @Override
            public AbsoluteDate getMinDate() {
                return AbsoluteDate.PAST_INFINITY;
            }

            @Override
            public double getMeanFlux(final AbsoluteDate date) throws PatriusException {
                return fbar;
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return AbsoluteDate.FUTURE_INFINITY;
            }

            @Override
            public double getInstantFlux(final AbsoluteDate date) throws PatriusException {
                return f;
            }

            @Override
            public double get24HoursKp(final AbsoluteDate date) throws PatriusException {
                return akp24;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do (test)
            }
        };
        final PVCoordinatesProvider sun = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final Vector3D pCIRF = FramesFactory.getITRF().getTransformTo(FramesFactory.getCIRF(), date)
                    .transformPosition(pos);
                final double c = MathLib.tan(hl - FastMath.PI);
                final double cst = (pCIRF.getX() + c * pCIRF.getY()) / (pCIRF.getY() - c * pCIRF.getX());
                return new PVCoordinates(new Vector3D(cst, 1., 0), new Vector3D(0, 0, 0));
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final AbstractDTM atmosphere = new DTM2012(params, sun, earth);

        return atmosphere.getData(date, pos, FramesFactory.getITRF());
    }

    /**
     * Compare a 10 period propagation with DTM2000 and propagation with DTM2012.
     * Check that final bulletin are close to each other (distance < 1km)
     */
    @Test
    public void testPropagationDTM2012vsDTM2000() throws PatriusException{
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double f = Constants.GRIM5C1_EARTH_FLATTENING;
        final ExtendedOneAxisEllipsoid earth = new ExtendedOneAxisEllipsoid(ae, f, FramesFactory.getITRF(), "Earth");
        final PVCoordinatesProvider sun = new MeeusSun();
        final DTMInputParameters params = new DTMInputParameters(){
            @Override
            public double getThreeHourlyKP(final AbsoluteDate date) throws PatriusException {
                return 1;
            }

            @Override
            public AbsoluteDate getMinDate() {
                return AbsoluteDate.PAST_INFINITY;
            }

            @Override
            public double getMeanFlux(final AbsoluteDate date) throws PatriusException {
                return 140;
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return AbsoluteDate.FUTURE_INFINITY;
            }

            @Override
            public double getInstantFlux(final AbsoluteDate date) throws PatriusException {
                return 140;
            }

            @Override
            public double get24HoursKp(final AbsoluteDate date) throws PatriusException {
                return 1;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do (test)
            }
        };

        // Propagation
        final SpacecraftState state2012 = propagate(new DTM2012(params, sun, earth));
        final SpacecraftState state2000 = propagate(new DTM2000(params, sun, earth));
        Assert.assertEquals(0, state2012.getPVCoordinates().getPosition().distance(state2000.getPVCoordinates().getPosition()), 1000);
    }

    /**
     * Compare a propagation with DTM2000 and propagation with DTM2012.
     * @throws PatriusException 
     */
    public SpacecraftState propagate(final ExtendedAtmosphere atmosphere) throws PatriusException{
        // the EME2000 frame
        final Frame EME2000Frame = FramesFactory.getEME2000();

        // initial date
        final AbsoluteDate initialDate = new AbsoluteDate(2003, 01, 01, TimeScalesFactory.getTAI());

        // initial orbit
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 411883.16;
        final double e = .0005670;
        final double i = FastMath.toRadians(51.98);
        final double pa = FastMath.toRadians(-33.75);
        final double raan = FastMath.toRadians(-70.);
        final double anomaly = FastMath.toRadians(-50.);
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngle.TRUE,
                EME2000Frame, initialDate, mu);
        
        // final date
        final AbsoluteDate finalDate = initialDate.shiftedBy(10 * initialOrbit.getKeplerianPeriod());

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("mainBody");
        builder.addProperty(new MassProperty(800.), "mainBody");
        builder.addProperty(new AeroSphereProperty(2., 2.7), "mainBody");
        Assembly assembly = builder.returnAssembly();

        // Aerol model
        final MassModel massModel = new MassModel(assembly);
        final AeroModel aero = new AeroModel(assembly);
        
        final DragForce drag = new DragForce(atmosphere, aero);
        
        // Loi d'attitude : pointage centre Terre
        final AttitudeLaw loi = new ConstantAttitudeLaw(EME2000Frame, Rotation.IDENTITY);
        
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(30.));
        propagator.setInitialState(new SpacecraftState(initialOrbit, loi.getAttitude(initialOrbit, initialDate, EME2000Frame), massModel));
        propagator.setMassProviderEquation(massModel);
        propagator.addForceModel(drag);
        
        return propagator.propagate(finalDate);
    }
}
