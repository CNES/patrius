/**
 * 
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
 * 
 * @history Created 04/04/2013
 * 
 * HISTORY
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:98:12/07/2013:Fixed wrong date parameter given to Attitude and DynamicsElements constructors
 * VERSION::FA:180:27/03/2014:Removed DynamicsElements - frames transformations derivatives unknown
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1287:13/11/2017: Integration problem in AngularVelocitiesPolynomialProfile.java
 * VERSION::DM:1951:10/12/2018: Creation Test RotationAcceleratioProfilesTest.java
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.profiles.AbstractAngularVelocitiesAttitudeProfile.AngularVelocityIntegrationType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link AngularVelocitiesPolynomialProfileLeg} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.4
 */
public class AngularVelocitiesPolynomialProfileLegTest {

    /**
     * @testedMethod {@link AngularVelocitiesPolynomialProfileLeg} methods
     * 
     * @description tests the various {@link AngularVelocitiesPolynomialProfileLeg} methods
     * 
     * @testPassCriteria results are as expected (functional test)
     * 
     * @referenceVersion 4.4
     * 
     * @nonRegressionVersion 4.4
     */
    @Test
    public final void testAngularVelocitiesPolynomialProfileLeg() throws PatriusException {
        
        // Initialization
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date0 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date1 = AbsoluteDate.J2000_EPOCH.shiftedBy(50);
        final AbsoluteDate date2 = AbsoluteDate.J2000_EPOCH.shiftedBy(100);

        final double[] x = { 0, 10, 20, 30, 40, 50 };
        final double[] y0 = { 2, 7, 4, 6, 9, 11 };
        final PolynomialFunctionLagrangeForm xpf1L = new PolynomialFunctionLagrangeForm(x, y0);
        final PolynomialFunction xpf1 = new PolynomialFunction(xpf1L.getCoefficients());
        final double[] y1 = { 3, 1, 5, 6, 8, 1 };
        final PolynomialFunctionLagrangeForm ypf1L = new PolynomialFunctionLagrangeForm(x, y1);
        final PolynomialFunction ypf1 = new PolynomialFunction(ypf1L.getCoefficients());
        final double[] y2 = { 1, 0, 10, 4, 2, 7 };
        final PolynomialFunctionLagrangeForm zpf1L = new PolynomialFunctionLagrangeForm(x, y2);
        final PolynomialFunction zpf1 = new PolynomialFunction(zpf1L.getCoefficients());

        final Rotation initialRotation = new Rotation(true, 1, 0, 0, 0);
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED, date0, date2, IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval interval2 = new AbsoluteDateInterval(
                IntervalEndpointType.CLOSED, date0.shiftedBy(20), date2.shiftedBy(-20), IntervalEndpointType.CLOSED);
        AngularVelocitiesPolynomialProfileLeg leg = new AngularVelocitiesPolynomialProfileLeg(
            xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        leg.setSpinDerivativesComputation(true);
        leg.clearCache();

        // Minor checks
        Assert.assertEquals(date0, leg.getDateZero());
        
        // Check all integration methods consistency (low order methods such as Wilcox 1 have limited accuracy)
        final AngularVelocitiesPolynomialProfileLeg leg1 = new AngularVelocitiesPolynomialProfileLeg(
                xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.WILCOX_1, 0.001);
        final AngularVelocitiesPolynomialProfileLeg leg2 = new AngularVelocitiesPolynomialProfileLeg(
                xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.WILCOX_2, 0.001);
        final AngularVelocitiesPolynomialProfileLeg leg3 = new AngularVelocitiesPolynomialProfileLeg(
                xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.WILCOX_3, 0.001);
        final AngularVelocitiesPolynomialProfileLeg leg4 = new AngularVelocitiesPolynomialProfileLeg(
                xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.WILCOX_4, 0.001);
        final AngularVelocitiesPolynomialProfileLeg leg5 = new AngularVelocitiesPolynomialProfileLeg(
                xpf1, ypf1, zpf1, gcrf, interval, initialRotation, date0, AngularVelocityIntegrationType.EDWARDS, 0.001);
        final Attitude actual1 = leg1.getAttitude(null, date1, FramesFactory.getGCRF());
        final Attitude actual2 = leg2.getAttitude(null, date1, FramesFactory.getGCRF());
        final Attitude actual3 = leg3.getAttitude(null, date1, FramesFactory.getGCRF());
        final Attitude actual4 = leg4.getAttitude(null, date1, FramesFactory.getGCRF());
        final Attitude actual5 = leg5.getAttitude(null, date1, FramesFactory.getGCRF());
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(actual5.getRotation(), actual1.getRotation())), 2E-2);
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(actual5.getRotation(), actual2.getRotation())), 1E-2);
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(actual5.getRotation(), actual3.getRotation())), 1E-6);
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(actual5.getRotation(), actual4.getRotation())), 1E-6);
        Assert.assertEquals(0, MathLib.toDegrees(Rotation.distance(actual5.getRotation(), actual5.getRotation())), 0);

        // Check copy
        final Attitude expected = leg.getAttitude(null, date1, FramesFactory.getGCRF());
        final AngularVelocitiesPolynomialProfileLeg leg10 = leg.copy(interval2);
        final Attitude actual = leg10.getAttitude(null, date1, FramesFactory.getGCRF());
        Assert.assertEquals(0, Rotation.distance(expected.getRotation(), actual.getRotation()), 2E-10);
        Assert.assertEquals(0, expected.getSpin().subtract(actual.getSpin()).getNorm(), 1E-15);
        Assert.assertEquals(0, expected.getRotationAcceleration().subtract(actual.getRotationAcceleration()).getNorm(), 1E-15);
    }
}
