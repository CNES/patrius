/**
 * 
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
 * @history Created 14/11/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Using normalized gravitational attraction.
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:464:24/06/2015:Analytical computation of the partial derivatives
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.BalminoAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.GRGSRL02FormatReader;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariableGravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince54Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation class for {@link VariablePotentialAttractionModel}
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialAttractionModelTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.3
 * 
 */
public class VariablePotentialAttractionModelTest {

    /**
     * Directory with data files
     */
    private final String add = "variablePotential";

    /**
     * threshold
     */
    private final double eps = Precision.EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle variable potential attraction
         * 
         * @featureDescription computation of the variable potential accelerations
         * 
         * @coveredRequirements DV-MOD_190, DV-MOD_220, DV-MOD_230
         */
        VARIABLE_POTENTIAL
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#getParametersNames()}
     * @testedMethod {@link VariablePotentialAttractionModel#getParameter(String)}
     * @testedMethod {@link VariablePotentialAttractionModel#setParameter()}
     * 
     * @description Test for the parameters
     * 
     * @input a parameter
     * 
     * @output its value
     * 
     * @testPassCriteria the parameter value is as expected exactly (0 ulp difference)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
        final VariablePotentialAttractionModel model = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            prov, 80, 80, 80, 80, true);

        double k = 5;
        Assert.assertEquals(2, model.getParameters().size());
        final ArrayList<Parameter> paramList = model.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            paramList.get(i).setValue(k);
            Assert.assertTrue(Precision.equals(k, paramList.get(i).getValue(), 0));
            k++;
        }
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#computeAcceleration(SpacecraftState)}
     * @testedMethod {@link VariablePotentialAttractionModel#addContribution(SpacecraftState, org.orekit.propagation.numerical.TimeDerivativesEquations)}
     * 
     * @description propagation test
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected positions
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testProp() throws IOException, ParseException, PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            prov, 80, 80, 80, 80, true);

        // prop
        final NumericalPropagator num = new NumericalPropagator(new DormandPrince54Integrator(.1, 60, new double[] {
            1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12 }, new double[] { 1e-11, 1e-11, 1e-11, 1e-13, 1e-13, 1e-13 }));

        // common spacecraft state
        final SpacecraftState scs = new SpacecraftState(new KeplerianOrbit(7100000, .0001, .1, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getTAI()),
            prov.getMu()));

        num.addForceModel(new NewtonianAttractionModel(prov.getMu()));
        num.addForceModel(grav);
        num.setInitialState(scs);

        // System.out.println("once : " +
        // num.propagate(scs.getDate().shiftedBy(1800)).getPVCoordinates().getPosition());

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel (Frame, VariablePotentialCoefficientsProvider, int, int, int, int, boolean)}
     * @testedMethod {@link VariablePotentialAttractionModel#computeAcceleration(AbsoluteDate, PVCoordinates)}
     * 
     * @description compare the magnitude of the computed acceleration to the reference acceleration, taking into
     *              account optional terms
     * 
     * @input a date and PV's
     * 
     * @output the computed acceleration
     * 
     * @testPassCriteria the computed acceleration is the same as the reference one, to 1e-14 m/s². The algorithms used
     *                   to computed the time elapsed since the reference date (potential file) are not the same, and a
     *                   bias is introduced onto the C and S corrections.
     * 
     *                   Reference result was modified to account for this bias, and allow for an accurate validation.
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testZoomFile() throws IOException, ParseException, PatriusException {

        FramesFactory.setConfiguration(Utils.getZOOMConfiguration());
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);

        // ref in itrf
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 04, 07, 02, 08.219, TimeScalesFactory.getTAI());
        final Vector3D refPos = new Vector3D(-2.7930686643474945E+06, 3.4121722068205951E+06, -5.0929827923869165E+06);
        final Vector3D refAcc = new Vector3D(3.6316227712497482E+00, -4.4366448118839950E+00, 6.6413794087929903E+00);
        final PVCoordinates refPVItrf = new PVCoordinates(refPos, Vector3D.ZERO);

        // frames
        final Frame itrf = FramesFactory.getITRF();

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(itrf, prov, 80, 80, 80, 80,
            true);

        // common spacecraft state

        // accelerations
        final Vector3D acc = grav.computeAcceleration(date, refPVItrf);
        final Vector3D accN = new NewtonianAttractionModel(itrf, prov.getMu()).computeAcceleration(refPVItrf, itrf,
            date);

        final Vector3D res = acc.add(accN);
        Assert.assertEquals(0, (refAcc.getX() - res.getX()) / refAcc.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (refAcc.getY() - res.getY()) / refAcc.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (refAcc.getZ() - res.getZ()) / refAcc.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel (Frame, VariablePotentialCoefficientsProvider, int, int)}
     * @testedMethod {@link VariablePotentialAttractionModel#computeAcceleration(SpacecraftState)}
     * 
     * @description compare the magnitude of the computed acceleration to the reference acceleration, without taking
     *              into account optional corrections
     * 
     * @input a date and PV's
     * 
     * @output the computed acceleration
     * 
     * @testPassCriteria computed acceleration is the same as the reference one with a 1e-14 m/s² tolerance. Reference
     *                   results are taking by making the GRGS RL02 file compatible with existing readers (removing the
     *                   optional terms) and using the usual CunninghamAttractionModel class to compute accelerations.
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testStaticpart() throws IOException, ParseException, PatriusException {

        // data root
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // providers
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "EIGEN-GRGS.RL02bis.MEAN-FIELD_static_part", true));
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader(
            "EIGEN-GRGS.RL02bis.MEAN-FIELD"));
        final PotentialCoefficientsProvider prov = GravityFieldFactory.getPotentialProvider();
        final VariablePotentialCoefficientsProvider provider = VariableGravityFieldFactory
            .getVariablePotentialProvider();

        // Variable model
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            provider, 150, 150);

        // Equivalent static model - denormalization is done with a more robust method
        final double[][] cStatic = prov.getC(150, 150, true);
        final double[][] sStatic = prov.getS(150, 150, true);
        // we ignore degrees and orders (0, 0), (1, 0) and (1, 1)
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < cStatic[j].length; i++) {
                cStatic[j][i] = 0;
            }
        }
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < sStatic[j].length; i++) {
                sStatic[j][i] = 0;
            }
        }
        final BalminoAttractionModel gravStatic = new BalminoAttractionModel(FramesFactory.getITRF(),
            provider.getAe(), provider.getMu(), cStatic, sStatic);

        // common spacecraft state
        final SpacecraftState scs = new SpacecraftState(new KeplerianOrbit(7100000, .0001, .1, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getTAI()),
            provider.getMu()));

        // accelerations
        final Vector3D acc = grav.computeAcceleration(scs);
        final Vector3D accR = gravStatic.computeAcceleration(scs);

        Assert.assertEquals(0, (acc.getX() - accR.getX()) / acc.getX(), this.eps);
        Assert.assertEquals(0, (acc.getY() - accR.getY()) / acc.getY(), this.eps);
        Assert.assertEquals(0, (acc.getZ() - accR.getZ()) / acc.getZ(), this.eps);

    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#getMu()}
     * @testedMethod {@link VariablePotentialAttractionModel#getAe()}
     * 
     * @description compare the constants from the provider
     * 
     * @input none
     * 
     * @output the constants (mu and ae)
     * 
     * @testPassCriteria the returned constants are the same as the ones from the file , to 1e-14 in relative precision.
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testConstants() throws IOException, ParseException, PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        final VariablePotentialCoefficientsProvider provider = VariableGravityFieldFactory
            .getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            provider, 50, 50);

        Assert.assertEquals(0, (grav.getMu() - provider.getMu()) / provider.getMu(), this.eps);
        Assert.assertEquals(0, (grav.getAe() - provider.getAe()) / provider.getAe(), this.eps);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#getEventsDetectors()}
     * @testedMethod {@link VariablePotentialAttractionModel#getParameter(String)}
     * @testedMethod {@link VariablePotentialAttractionModel#getParametersNames()}
     * @testedMethod {@link VariablePotentialAttractionModel#setParameter(String, double)}
     * 
     * @description coverage of unimplemented methods
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria expected exceptions
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testParams() throws IOException, ParseException, PatriusException {

        final Parameter testStr = new Parameter("test", 0.);
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        final VariablePotentialCoefficientsProvider provider = VariableGravityFieldFactory
            .getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            provider, 50, 50);

        Assert.assertTrue(grav.getEventsDetectors().length == 0);
        Assert.assertEquals(2, grav.getParameters().size());
        Assert.assertFalse(grav.supportsParameter(testStr));
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel(Frame, VariablePotentialCoefficientsProvider, int, int)}
     * 
     * @description coverga of constructor exceptions
     * 
     * @input bad parameters
     * 
     * @output exceptions
     * 
     * @testPassCriteria OrekitException
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testConstructorExceptions() throws IOException, ParseException, PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        final VariablePotentialCoefficientsProvider provider = VariableGravityFieldFactory
            .getVariablePotentialProvider();
        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 200, 200);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 80, 100);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 200, 5);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 5, 200);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 7, 2, false);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 2, 7, false);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 5, 5, 5, 5, 7, 5, false);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        try {
            new VariablePotentialAttractionModel(FramesFactory.getITRF(),
                provider, 5, 5, 5, 5, 5, 5, 5, 7, false);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /*
     * BELOW IS THE CODE USED TO MODIFY THE DATES IN THE FILES GENERATED BY ZOOM
     * The algorithms used to computed the time elapsed since the reference date (potential file) are not the same, and
     * a bias is introduced onto the C and S corrections.
     * Reference files were modified to account for this bias. A 5e-13 m/s² error remains and without this bias, the
     * accuracy of the implemented force model is about 1e-15 m/s².
     */

    // private static double getCNESJd(final AbsoluteDate date) throws OrekitException {
    //
    // // Reference epoch
    // final AbsoluteDate ref = new AbsoluteDate(new DateTimeComponents(DateComponents.FIFTIES_EPOCH,
    // TimeComponents.H00), TimeScalesFactory.getUTC());
    //
    // // Return offset from reference epoch in days
    // return date.offsetFrom(ref, TimeScalesFactory.getUTC()) / 86400;
    // }
    //
    // private static AbsoluteDate getDate(final double cnesJDate, final double secs) throws OrekitException {
    //
    // // Reference epoch
    // final AbsoluteDate ref = new AbsoluteDate(new DateTimeComponents(DateComponents.FIFTIES_EPOCH,
    // TimeComponents.H00), TimeScalesFactory.getUTC());
    //
    // //
    // return new AbsoluteDate(ref, cnesJDate * Constants.JULIAN_DAY + secs, TimeScalesFactory.getUTC());
    // }
    //
    // private double anneeFractionnaire(final int JJUL, final double SS) {
    //
    // int R0 = JJUL - 1 - 1721119 + 2433283;
    // int Q = R0 / 146097;
    // int RS = (R0 - 146097 * Q) / 36524;
    // RS = (3 * RS + 3) / 4;
    // int C = Q * 4 + RS;
    // int R1 = R0 - (146097 * C) / 4;
    // Q = R1 / 1461;
    // int RA = (R1 - 1461 * Q) / 365;
    // RA = (3 * RA + 3) / 4;
    // int YA = Q * 4 + RA;
    // int R2 = R1 - (1461 * YA) / 4;
    // Q = R2 / 153;
    // int R = (R2 - 153 * Q - 1) / 30;
    // R = (4 * R + 4) / 5;
    // int MOIS = Q * 5 + R;
    //
    // int JOURS_AN;
    // int JOURS_AN_A;
    //
    // if (YA == 99) {
    // JOURS_AN = (1461 + RS) / 4;
    // } else {
    // JOURS_AN = (1461 + RA) / 4;
    // }
    // if (YA - 1 == 99) {
    // JOURS_AN_A = 366 - (RS + 3) / 4;
    // } else {
    // JOURS_AN_A = 366 - (RA + 3) / 4;
    // }
    // double ANNEES = 100 * C + YA;
    // if (MOIS >= 10) {
    // ANNEES = ANNEES + 1;
    // R2 = R2 - 306;
    // } else {
    // R2 = R2 + JOURS_AN_A - 306;
    // JOURS_AN = JOURS_AN_A;
    // }
    // double FRAC = ((double) R2 + ((double) SS) / Constants.JULIAN_DAY) / ((double) JOURS_AN);
    // double R_JULIEN_ANNEE = (double) ANNEES + FRAC;
    //
    // return R_JULIEN_ANNEE;
    //
    // }
    //
    // @Test
    // public void vals() throws IllegalArgumentException, OrekitException, IOException {
    // CNESUtils.clearNewFactoriesAndCallSetDataRoot(add);
    //
    // final int firstDate = 20152;
    // final double secs = 1440;
    // final AbsoluteDate ref = new AbsoluteDate(2004, 1, 1, TimeScalesFactory.getUTC());
    //
    // double zoomVal = anneeFractionnaire(firstDate, 0) - 2004;
    // double orekitVal = getDate(firstDate, 0).offsetFrom(ref, TimeScalesFactory.getUTC()) / Constants.JULIAN_YEAR;
    //
    // double offset = (zoomVal - orekitVal) * Constants.JULIAN_YEAR;
    //
    // final File root = new File("src/test/resources/variablePotential");
    //
    // int i = 0;
    // for (File f : root.listFiles()) {
    // System.out.println(i++ + " " + f);
    // }
    //
    // final File originalFile = root.listFiles()[7];
    // final File newFile = new File("src/test/resources/variablePotential/new_eph");
    //
    // System.out.println(originalFile);
    //
    // FileInputStream st = new FileInputStream(originalFile);
    // FileOutputStream ot = new FileOutputStream(newFile);
    //
    // BufferedReader rd = new BufferedReader(new InputStreamReader(st));
    // BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(ot));
    //
    // // sub-patterns
    // final String jd = "([0-9]{5})";
    // final String sec = "([0-9]{1,5}.[0-9]{2})";
    // final String real = "([-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d)";
    // final String sep = "\\s*";
    // final String data = "^" + jd + sep + sec + sep + real + sep + real + sep + real + "*$";
    //
    // final Pattern pattern = Pattern.compile(data);
    // System.out.println(pattern);
    // Matcher match;
    //
    // int fileDay;
    // double fileSecs;
    //
    // AbsoluteDate newDate;
    // int newDay;
    // double newSecs;
    //
    // double x;
    // double y;
    // double z;
    //
    // UTCScale utc = TimeScalesFactory.getUTC();
    //
    // String newLine = "";
    //
    // final DecimalFormat d2 = new DecimalFormat("00000.00");
    // final DecimalFormat s16 = new DecimalFormat("0.00000000000000000E00");
    //
    // System.out.println(s16.format(1));
    //
    // double rm = 1440;
    //
    // for (String line = rd.readLine(); line != null; line = rd.readLine()) {
    //
    // System.out.println("OLD : " + line);
    //
    // match = pattern.matcher(line);
    // match.matches();
    // // System.out.print("  ");
    // // System.out.println("1  " + match.group(1).trim());
    // // System.out.println("2  " + match.group(2).trim());
    // // System.out.println("3  " + match.group(3).trim());
    // // System.out.println("4  " + match.group(4).trim());
    // // System.out.println("5  " + match.group(5).trim());
    //
    // fileDay = Integer.parseInt(match.group(1).trim());
    // fileSecs = Double.parseDouble(match.group(2).trim());
    //
    // x = Double.parseDouble(match.group(3).trim());
    // y = Double.parseDouble(match.group(4).trim());
    // z = Double.parseDouble(match.group(5).trim());
    //
    // // due to zoom's algo, seconds not taken into account
    // if (fileSecs >= rm) {
    // fileSecs -= rm;
    // } else {
    // fileDay -= 1;
    // fileSecs = 86400 - (rm - fileSecs);
    // }
    //
    // // introduce offset
    // newDate = new AbsoluteDate(getDate(fileDay, fileSecs), offset, TimeScalesFactory.getTAI());
    //
    // // cnes jd and secs
    // newDay = (int) FastMath.floor(getCNESJd(newDate));
    // newSecs = (getCNESJd(newDate) - FastMath.floor(getCNESJd(newDate))) * Constants.JULIAN_DAY;
    //
    // newLine += newDay;
    // newLine += " ";
    // newLine += newSecs;
    // newLine += " " + s16.format(x).replace(",", ".") + " " + s16.format(y).replace(",", ".") + " "
    // + s16.format(z).replace(",", ".");
    // newLine += "\n";
    //
    // System.out.println("TME : " + newSecs);
    // System.out.print("NEW : " + newLine);
    //
    // wr.write(newLine);
    //
    // newLine = "";
    // }
    //
    // wr.close();
    // rd.close();
    //
    // }

    /*
     * PROPAGATION TESTS FOR MU
     */
    // @Test
    // public void testProp() throws IOException, ParseException, OrekitException {
    //
    // FramesConfigurationBuilder.setZOOMConfiguration();
    // CNESUtils.clearNewFactoriesAndCallSetDataRoot(add);
    //
    // // frames
    // final Frame itrf = FramesFactory.getITRF();
    //
    // // Variable model
    // VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
    // final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
    // final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
    // prov, 80, 80, 80, 80, true);
    //
    // // prop
    // final NumericalPropagator num = new NumericalPropagator(new DormandPrince54Integrator(.1, 60, new double[] {
    // 1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12, 1e-13 }, new double[] { 1e-11, 1e-11, 1e-11, 1e-13, 1e-13,
    // 1e-13, 1e-15 }));
    //
    // // common spacecraft state
    // final SpacecraftState scs = new SpacecraftState(new KeplerianOrbit(7100000, .0001, .1, 0, 0, 0,
    // PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, prov.getMu()));
    //
    // num.addForceModel(new NewtonianAttraction(prov.getMu()));
    // num.addForceModel(grav);
    // num.setInitialState(scs);
    //
    // System.out.println("once : " + num.propagate(scs.getDate().shiftedBy(5*86400)).getPVCoordinates().getPosition());
    //
    // }
    //
    // @Test
    // public void testProp1() throws IOException, ParseException, OrekitException {
    //
    // FramesConfigurationBuilder.setZOOMConfiguration();
    // CNESUtils.clearNewFactoriesAndCallSetDataRoot(add);
    //
    // // frames
    // final Frame itrf = FramesFactory.getITRF();
    //
    // // Variable model
    // VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
    // final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
    // final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
    // prov, 80, 80, 80, 80, false);
    //
    // // prop
    // final NumericalPropagator num = new NumericalPropagator(new DormandPrince54Integrator(.1, 60, new double[] {
    // 1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12, 1e-13 }, new double[] { 1e-11, 1e-11, 1e-11, 1e-13, 1e-13,
    // 1e-13, 1e-15 }));
    //
    // // common spacecraft state
    // final SpacecraftState scs = new SpacecraftState(new KeplerianOrbit(7100000, .0001, .1, 0, 0, 0,
    // PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, prov.getMu()));
    //
    // num.addForceModel(new NewtonianAttraction(prov.getMu()));
    // num.addForceModel(grav);
    // num.setInitialState(scs);
    //
    // System.out.println("very : " + num.propagate(scs.getDate().shiftedBy(5*86400)).getPVCoordinates().getPosition());
    //
    // }
    //
    // @Test
    // public void testProp2() throws IOException, ParseException, OrekitException {
    //
    // FramesConfigurationBuilder.setZOOMConfiguration();
    // CNESUtils.clearNewFactoriesAndCallSetDataRoot(add);
    //
    // // frames
    // final Frame itrf = FramesFactory.getITRF();
    //
    // // Variable model
    // VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
    // final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
    // final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
    // prov, 80, 80);
    //
    // // prop
    // final NumericalPropagator num = new NumericalPropagator(new DormandPrince54Integrator(.1, 60, new double[] {
    // 1e-9, 1e-9, 1e-9, 1e-12, 1e-12, 1e-12, 1e-13 }, new double[] { 1e-11, 1e-11, 1e-11, 1e-13, 1e-13,
    // 1e-13, 1e-15 }));
    //
    // // common spacecraft state
    // final SpacecraftState scs = new SpacecraftState(new KeplerianOrbit(7100000, .0001, .1, 0, 0, 0,
    // PositionAngle.MEAN, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, prov.getMu()));
    //
    // num.addForceModel(new NewtonianAttraction(prov.getMu()));
    // num.addForceModel(grav);
    // num.setInitialState(scs);
    //
    // System.out.println("nocor : " +
    // num.propagate(scs.getDate().shiftedBy(5*86400)).getPVCoordinates().getPosition());
    //
    // }

    /**
     * FA 284 : added test to addContribution
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#AddContribution()}
     * 
     * @description Test for the method AddContribution
     * 
     * @testPassCriteria regression upon reference values
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testAddContribution() throws PatriusException, IOException, ParseException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            prov, 80, 80, 80, 80, true);

        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = date.shiftedBy(10.);

        // mu
        final double mu = 3.9860043770442000E+14;

        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(-7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, mu);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final NumericalPropagator calc = new NumericalPropagator(
            new GraggBulirschStoerIntegrator(10.0, 30.0, 0, 1.0e-5));
        calc.addForceModel(grav);

        calc.setInitialState(scr);
        calc.propagate(finalDate);

    }

    /**
     * Additional partial derviatives tests to ensure the jacobian is correctly taken into account.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException {

        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // force model: tides with all corrections
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final Frame gcrf = FramesFactory.getGCRF();

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            prov, 80, 80, 80, 80, true);

        // orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE, gcrf, date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // partial derivatives
        final double[][] dAccdPos = new double[6][6];
        grav.addDAccDState(state, dAccdPos, new double[6][6]);

        /*
         * ======================================
         * finite diff _ DELTAS IN GCRF
         */
        final Vector3D pos = orbit.getPVCoordinates(gcrf).getPosition();
        final Vector3D vel = orbit.getPVCoordinates(gcrf).getVelocity();

        /* ===================================== */

        final double dh = .5;

        // positions
        final Vector3D ppx = pos.add(Vector3D.PLUS_I.scalarMultiply(dh));
        final Vector3D ppy = pos.add(Vector3D.PLUS_J.scalarMultiply(dh));
        final Vector3D ppz = pos.add(Vector3D.PLUS_K.scalarMultiply(dh));

        final Vector3D pmx = pos.add(Vector3D.PLUS_I.scalarMultiply(-dh));
        final Vector3D pmy = pos.add(Vector3D.PLUS_J.scalarMultiply(-dh));
        final Vector3D pmz = pos.add(Vector3D.PLUS_K.scalarMultiply(-dh));

        // pv coordinates
        final PVCoordinates pvpx = new PVCoordinates(ppx, vel);
        final PVCoordinates pvpy = new PVCoordinates(ppy, vel);
        final PVCoordinates pvpz = new PVCoordinates(ppz, vel);

        final PVCoordinates pvmx = new PVCoordinates(pmx, vel);
        final PVCoordinates pvmy = new PVCoordinates(pmy, vel);
        final PVCoordinates pvmz = new PVCoordinates(pmz, vel);

        // orbits
        final CartesianOrbit opx = new CartesianOrbit(pvpx, gcrf, date, mu);
        final CartesianOrbit opy = new CartesianOrbit(pvpy, gcrf, date, mu);
        final CartesianOrbit opz = new CartesianOrbit(pvpz, gcrf, date, mu);

        final CartesianOrbit omx = new CartesianOrbit(pvmx, gcrf, date, mu);
        final CartesianOrbit omy = new CartesianOrbit(pvmy, gcrf, date, mu);
        final CartesianOrbit omz = new CartesianOrbit(pvmz, gcrf, date, mu);

        // states
        final SpacecraftState sspx = new SpacecraftState(opx);
        final SpacecraftState sspy = new SpacecraftState(opy);
        final SpacecraftState sspz = new SpacecraftState(opz);

        final SpacecraftState ssmx = new SpacecraftState(omx);
        final SpacecraftState ssmy = new SpacecraftState(omy);
        final SpacecraftState ssmz = new SpacecraftState(omz);

        // acc
        final Vector3D apx = grav.computeAcceleration(sspx);
        final Vector3D apy = grav.computeAcceleration(sspy);
        final Vector3D apz = grav.computeAcceleration(sspz);

        final Vector3D amx = grav.computeAcceleration(ssmx);
        final Vector3D amy = grav.computeAcceleration(ssmy);
        final Vector3D amz = grav.computeAcceleration(ssmz);

        // pds
        final Vector3D pdx = apx.subtract(amx).scalarMultiply(1 / (2 * dh));
        final Vector3D pdy = apy.subtract(amy).scalarMultiply(1 / (2 * dh));
        final Vector3D pdz = apz.subtract(amz).scalarMultiply(1 / (2 * dh));

        final double[][] acc = { pdx.toArray(), pdy.toArray(), pdz.toArray() };
        final double[][] tacc = this.transpose(acc);

        System.out.println("FINITE DIFFERENCES");
        this.print(tacc);

        final double[][] diff = new double[3][3];
        for (int ii = 0; ii < diff.length; ii++) {
            for (int j = 0; j < diff[ii].length; j++) {
                diff[ii][j] = (dAccdPos[ii][j] - tacc[ii][j]) / dAccdPos[ii][j];
                Assert.assertEquals(0, diff[ii][j], 2e-4);
            }
        }
        System.out.println();
        System.out.println("RELATIVE DIFFRENCE");
        this.print(diff);

        try {
            final double[] dAccdParam = new double[3];

            grav.addDAccDParam(state, new Parameter("toto", 1.), dAccdParam);
        } catch (final PatriusException exp) {
            // expected
        }

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VARIABLE_POTENTIAL}
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel(Frame, VariablePotentialCoefficientsProvider, boolean, int, int, int, int)}
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel(Frame, VariablePotentialCoefficientsProvider, int, int, int, int, int, int, boolean, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link VariablePotentialAttractionModel}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction :
     *                   instantiation is done with degree = order = 0 for C and S coefficients used in this computation
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException, IOException, ParseException {

        // Frame
        final Frame gcrf = FramesFactory.getGCRF();

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // Forcee model: tides with all corrections
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Variable model
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory.getVariablePotentialProvider();

        // use same degree/order for partial derivatives than acceleration
        final VariablePotentialAttractionModel grav = new VariablePotentialAttractionModel(FramesFactory.getITRF(),
            prov, 80, 80, 0, 0);

        final VariablePotentialAttractionModel grav2 = new VariablePotentialAttractionModel(gcrf, prov, 80, 80, 0, 0,
            80, 80, 0, 0, true);

        // Check partial derivatives computation is well deactivated
        Assert.assertFalse(grav.computeGradientPosition());
        // Partial derivatives wrt velocity are always null in variable potential model
        Assert.assertFalse(grav.computeGradientVelocity());

        // Orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE, gcrf, date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Partial derivatives
        final double[][] dAccdPos = new double[6][6];
        final double[][] dAccdVel = new double[6][6];
        final double[][] dAccdPos2 = new double[6][6];
        final double[][] dAccdVel2 = new double[6][6];
        grav.addDAccDState(state, dAccdPos, dAccdVel);
        grav2.addDAccDState(state, dAccdPos2, dAccdVel2);

        // Check all derivatives are null
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(0, dAccdPos[j][k], 0);
                Assert.assertEquals(0, dAccdVel[j][k], 0);
                Assert.assertEquals(0, dAccdPos2[j][k], 0);
                Assert.assertEquals(0, dAccdVel2[j][k], 0);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel(Frame, VariablePotentialCoefficientsProvider, boolean, int, int, int, int)}
     * @testedMethod {@link VariablePotentialAttractionModel#VariablePotentialAttractionModel(Frame, VariablePotentialCoefficientsProvider, int, int, int, int, int, int, boolean, boolean)}
     * 
     * @description This test checks that:
     *              <ul>
     *              <li>The numerical propagation of a given orbit using instances of VariablePotentialAttractionModel
     *              with fixed degree/order (60, 60) for acceleration but different degree/order (60, 60) and (59, 59)
     *              for partial derivatives lead to the same [position, velocity] state but slighty different state
     *              transition matrix.</li>
     *              <li>The partial derivatives of model (60, 60) for acceleration and (59, 59) for partial derivatives
     *              are the same than of model (59, 59) for acceleration and (59, 59) for partial derivatives. This test
     *              is performed for static and dynamic parts.</li>
     *              <ul>
     * 
     * @input instances of {@link VariablePotentialAttractionModel}
     * 
     * @output positions, velocities of final orbits, partials derivatives
     * 
     * @testPassCriteria the [positions, velocities] must be equals, state transition matrix "almost" the same (relative
     *                   difference < 1E-2)
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationDifferentDegreeOrder() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // date
        final AbsoluteDate date = new AbsoluteDate();

        // constants
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // SpacecraftState
        final KeplerianOrbit orbit = new KeplerianOrbit(7E6, 0.001, 0.93, 0, 0, 0, PositionAngle.TRUE, gcrf, date, mu);
        SpacecraftState state1 = new SpacecraftState(orbit);
        SpacecraftState state2 = new SpacecraftState(orbit);
        final double t = orbit.getKeplerianPeriod();

        // gravity
        VariableGravityFieldFactory.addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider provider = VariableGravityFieldFactory
            .getVariablePotentialProvider();

        // Create 3 instances of VariablePotentialAttractionModel with different degrees/orders (static part)
        final VariablePotentialAttractionModel model1 = new VariablePotentialAttractionModel(itrf, provider, 60, 60,
            60, 60);
        final VariablePotentialAttractionModel model2 = new VariablePotentialAttractionModel(itrf, provider, 60, 60,
            59, 59, 59, 59, 59, 59, false);
        final VariablePotentialAttractionModel model3 = new VariablePotentialAttractionModel(itrf, provider, 59, 59,
            59, 59, 59, 59, 59, 59, false);

        // Create 2 instances of VariablePotentialAttractionModel with different degrees/orders (dynamic part)
        final VariablePotentialAttractionModel model4 = new VariablePotentialAttractionModel(itrf, provider, 60, 60,
            60, 60, 60, 60, 60, 60, false);
        final VariablePotentialAttractionModel model5 = new VariablePotentialAttractionModel(itrf, provider, 60, 60,
            60, 60, 59, 59, 60, 60, false);

        // Propagators
        final double step = 60;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);
        final NumericalPropagator prop1 = new NumericalPropagator(integrator);
        final NumericalPropagator prop2 = new NumericalPropagator(integrator);

        final PartialDerivativesEquations eq1 = new PartialDerivativesEquations("partial", prop1);
        state1 = eq1.setInitialJacobians(state1);
        prop1.setInitialState(state1);
        prop1.addForceModel(model1);
        final PartialDerivativesEquations eq2 = new PartialDerivativesEquations("partial", prop2);
        state2 = eq2.setInitialJacobians(state2);
        prop2.setInitialState(state2);
        prop2.addForceModel(model2);

        // Propagation : final state
        final SpacecraftState FinalState1 = prop1.propagate(date.shiftedBy(t));
        final SpacecraftState FinalState2 = prop2.propagate(date.shiftedBy(t));

        // Positions and velocities must be the same whereas degrees/orders are different for each model
        final Vector3D pos1 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D pos2 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D vel1 = FinalState2.getPVCoordinates().getVelocity();
        final Vector3D vel2 = FinalState2.getPVCoordinates().getVelocity();

        Assert.assertEquals(0., pos1.distance(pos2), 0.);
        Assert.assertEquals(0., vel1.distance(vel2), 0.);

        // Check that partial derivatives are different, but "nearly" the same
        final double epsilon = 2.0E-2;
        final double[] stm1 = FinalState1.getAdditionalState("partial");
        final double[] stm2 = FinalState2.getAdditionalState("partial");
        for (int i = 0; i < stm1.length; i++) {
            Assert.assertEquals(0., (stm1[i] - stm2[i]) / stm1[i], epsilon);
            Assert.assertFalse(stm1[i] == stm2[i]);
        }

        // Check that different instances of VariablePotentialAttractionModel returns same partial derivatives
        final double[][] dAccdPos = new double[6][6];
        final double[][] dAccdVel = new double[6][6];
        final double[][] dAccdPos2 = new double[6][6];
        final double[][] dAccdVel2 = new double[6][6];
        final double[][] dAccdPos3 = new double[6][6];
        final double[][] dAccdVel3 = new double[6][6];
        final double[][] dAccdPos4 = new double[6][6];
        final double[][] dAccdVel4 = new double[6][6];
        model2.addDAccDState(state1, dAccdPos, dAccdVel);
        model3.addDAccDState(state1, dAccdPos2, dAccdVel2);
        model4.addDAccDState(state1, dAccdPos3, dAccdVel3);
        model5.addDAccDState(state1, dAccdPos4, dAccdVel4);

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(dAccdPos[j][k], dAccdPos2[j][k], 0);
                Assert.assertEquals(dAccdVel[j][k], dAccdVel2[j][k], 0);
                Assert.assertEquals(dAccdPos3[j][k], dAccdPos4[j][k], 0);
                Assert.assertEquals(dAccdVel3[j][k], dAccdVel4[j][k], 0);
            }
        }
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException, IOException,
            ParseException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot(this.add);
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Variable model
        VariableGravityFieldFactory
                .addVariablePotentialCoefficientsReader(new GRGSRL02FormatReader("EGNGL4S_21"));
        final VariablePotentialCoefficientsProvider prov = VariableGravityFieldFactory
                .getVariablePotentialProvider();
        final VariablePotentialAttractionModel forceModel = new VariablePotentialAttractionModel(
                FramesFactory.getITRF(), prov, 80, 80, 80, 80, true);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL,
                VariablePotentialAttractionModel.class));
        }
    }

    double[][] transpose(final double[][] d) {

        final double[][] dt = new double[d[0].length][d.length];

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                dt[j][i] = d[i][j];
            }
        }

        return dt;

    }

    void print(final double[][] d) {
        for (final double[] row : d) {
            for (final double e : row) {
                System.out.printf("%.16e\t", e);
            }
            System.out.println();
        }
    }

}
