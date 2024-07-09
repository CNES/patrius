/**
 * Copyright 2011-2017 CNES
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
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1950:14/11/2018:new attitude profile design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.SunPointing;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the profiles package.
 *              </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: AeroAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 4.2
 */
public class AttitudeProfileTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle attitude profiles
         * 
         * @featureDescription object describing an attitude profile
         * 
         * @coveredRequirements DM-1950
         */
        ATTITUDE_PROFILES
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AttitudeProfileTest.class.getSimpleName(), "Attitude profile");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_PROFILES}
     * 
     * @testedMethod {@link AttitudeProfile#getAttitude()}
     * 
     * @description test attitude computation methods of attitude profile sequence
     * 
     * @input an attitude profile sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output orientation at different dates
     * 
     * @testPassCriteria rotation is as expected (reference: math)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testNumericalAttitudeProfilesSequence() throws PatriusException {

        // Initialization
        Report.printMethodHeader("testNumericalAttitudeProfilesSequence", "Angle computation", "Math", 1E-14,
            ComparisonType.ABSOLUTE);

        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(7000000, 6000000, 5000000), new Vector3D(1000, 7000, 2000));
        final Vector3D pos = pv.getPosition();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return pv;
            }
        };

        // Build profile
        final AttitudeProfilesSequence provider = new AttitudeProfilesSequence("TEST");
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbstractAttitudeProfile leg1 = new WrapperProfile(new BodyCenterPointing(), date1, date2);
        final AbstractAttitudeProfile leg2 =
            new WrapperProfile(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()), date2, date3);
        final AbstractAttitudeProfile leg3 =
            new WrapperProfile(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY), date3, date4);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        // Computation and check angle on each segment
        // Each segment performs a rotation of [0, 0, 1] vector
        final Rotation rot1 =
            provider.getAttitude(pvProvider, date1.shiftedBy(1), FramesFactory.getGCRF()).getRotation();
        final Rotation rot2 =
            provider.getAttitude(pvProvider, date2.shiftedBy(1), FramesFactory.getGCRF()).getRotation();
        final Rotation rot3 =
            provider.getAttitude(pvProvider, date3.shiftedBy(1), FramesFactory.getGCRF()).getRotation();
        final Vector3D actual1 = rot1.applyTo(Vector3D.PLUS_K);
        final Vector3D actual2 = rot2.applyTo(Vector3D.PLUS_K);
        final Vector3D actual3 = rot3.applyTo(Vector3D.PLUS_K);
        final Vector3D expected1 = pos.normalize().negate();
        final Vector3D expected2 =
            new MeeusSun().getPVCoordinates(date2.shiftedBy(1), FramesFactory.getGCRF()).getPosition().subtract(pos)
                .normalize();
        final Vector3D expected3 = Vector3D.PLUS_K;
        Report.printToReport("Image of +K on 1st segment", expected1, actual1);
        Report.printToReport("Image of +K on 2nd segment", expected2, actual2);
        Report.printToReport("Image of +K on 3rd segment", expected3, actual3);
        Assert.assertEquals(0., expected1.subtract(actual1).getNorm(), 1E-6);
        Assert.assertEquals(0., expected2.subtract(actual2).getNorm(), 1E-6);
        Assert.assertEquals(0., expected3.subtract(actual3).getNorm(), 1E-6);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#LEGS_SEQUENCE}
     * 
     * @testedMethod all attitude profile sequence methods (set methods not included)
     * 
     * @description test all functional methods of attitude profile sequence (set methods not included)
     * 
     * @input an attitude profile sequence:
     *        - Body center pointing rotation on [AbsoluteDate.J2000_EPOCH; AbsoluteDate.J2000_EPOCH + 10s]
     *        - Sun pointing on [AbsoluteDate.J2000_EPOCH + 10s; AbsoluteDate.J2000_EPOCH + 20s]
     *        - Constant rotation on [AbsoluteDate.J2000_EPOCH + 20s; AbsoluteDate.J2000_EPOCH + 30s]
     * 
     * @output output of all methods
     * 
     * @testPassCriteria result is as expected (functional test)
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testFunctionalAttitudeProfilesSequence() throws PatriusException {

        // Initialization
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(7000000, 6000000, 5000000), new Vector3D(1000, 7000, 2000));
        final Vector3D pos = pv.getPosition();
        final PVCoordinatesProvider pvProvider = new PVCoordinatesProvider(){
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return pv;
            }
        };

        // Build profile
        final AttitudeProfilesSequence provider = new AttitudeProfilesSequence("TEST");
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(0.);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate date3 = AbsoluteDate.J2000_EPOCH.shiftedBy(20.);
        final AbsoluteDate date4 = AbsoluteDate.J2000_EPOCH.shiftedBy(30.);
        final AbstractAttitudeProfile leg1 = new WrapperProfile(new BodyCenterPointing(), date1, date2);
        final AbstractAttitudeProfile leg2 =
            new WrapperProfile(new SunPointing(Vector3D.PLUS_K, Vector3D.PLUS_I, new MeeusSun()), date2, date3);
        final AbstractAttitudeProfile leg3 =
            new WrapperProfile(new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY), date3, date4);
        provider.add(leg1);
        provider.add(leg2);
        provider.add(leg3);

        // Check time interval
        Assert.assertEquals(0, provider.getTimeInterval().getLowerData().durationFrom(date1), 0);
        Assert.assertEquals(0, provider.getTimeInterval().getUpperData().durationFrom(date4), 0);

        // Check nature
        Assert.assertEquals("TEST", provider.getNature());
        Assert.assertEquals("ATTITUDE_PROFILE", leg1.getNature());

        // Check dates
        leg1.checkDate(AbsoluteDate.J2000_EPOCH.shiftedBy(0));
        try {
            leg1.checkDate(AbsoluteDate.J2000_EPOCH.shiftedBy(-10));
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check spin derivative computation
        final Attitude attitude1 = provider.getAttitude(pvProvider, date1.shiftedBy(1), FramesFactory.getGCRF());
        Assert.assertNull(attitude1.getRotationAcceleration());
        provider.setSpinDerivativesComputation(true);
        final Attitude attitude2 = provider.getAttitude(pvProvider, date1.shiftedBy(1), FramesFactory.getGCRF());
        Assert.assertNotNull(attitude2.getRotationAcceleration());

        // Check exception (date outside interval)
        try {
            provider.getAttitude(pvProvider, date1.shiftedBy(-1), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            provider.getAttitude(pvProvider, date4.shiftedBy(1), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Test 2nd attitude computation method
        final Orbit orbit =
            new CartesianOrbit(pv, FramesFactory.getGCRF(), date2.shiftedBy(1), Constants.EGM96_EARTH_MU);
        final Rotation rot2 = provider.getAttitude(orbit).getRotation();
        final Rotation rot3 = leg2.getAttitude(orbit).getRotation();
        final Vector3D actual2 = rot2.applyTo(Vector3D.PLUS_K);
        final Vector3D actual3 = rot3.applyTo(Vector3D.PLUS_K);
        final Vector3D expected2 =
            new MeeusSun().getPVCoordinates(date2.shiftedBy(1), FramesFactory.getGCRF()).getPosition().subtract(pos)
                .normalize();
        new MeeusSun().getPVCoordinates(date2.shiftedBy(1), FramesFactory.getGCRF()).getPosition().subtract(pos)
            .normalize();
        Assert.assertEquals(0., expected2.subtract(actual2).getNorm(), 1E-6);
        Assert.assertEquals(0., expected2.subtract(actual3).getNorm(), 1E-6);
    }

    /**
     * Linear leg: orientation = a.(t - t0) + b.
     */
    private class WrapperProfile extends AbstractAttitudeProfile {

        /** Serial UID. */
        private static final long serialVersionUID = 1L;

        /** Law. */
        private final AttitudeLaw law;

        /**
         * Constructor
         * 
         * @param law attitude law
         * @param start start date of the profile
         * @param end end date of the profile
         */
        public WrapperProfile(final AttitudeLaw law, final AbsoluteDate start, final AbsoluteDate end) {
            super(new AbsoluteDateInterval(start, end));
            this.law = law;
        }

        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)                                                                                                           throws PatriusException {
            return this.law.getAttitude(pvProv, date, frame);
        }

        @Override
        public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
            this.law.setSpinDerivativesComputation(computeSpinDerivatives);
        }

        @Override
        public WrapperProfile copy(final AbsoluteDateInterval newInterval) {
            return new WrapperProfile(law, newInterval.getLowerData(), newInterval.getUpperData());
        }
    }
}
