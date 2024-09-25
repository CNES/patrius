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
 * @history creation 26/04/2012
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.MainPart;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              test class for the simple and the computed inertia model.
 *              </p>
 * 
 * @see InertiaComputedModel
 * @see InertiaSimpleModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class InertiaModelTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Inertia simple model
         * 
         * @featureDescription Inertia simple model that does not need an assembly.
         * 
         * @coveredRequirements DV-VEHICULE_120
         */
        INERTIA_SIMPLE_MODEL,

        /**
         * @featureTitle Inertia computed model
         * 
         * @featureDescription Inertia computed model, from an assembly
         * 
         * @coveredRequirements DV-VEHICULE_120, DV-VEHICULE_130, DV-VEHICULE_140, DV-VEHICULE_150,
         *                      DV-VEHICULE_170
         */
        INERTIA_COMPUTED_MODEL,
    }

    /**
     * J2000 date
     */
    private final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    /** reference Frame */
    private final Frame refFrame = FramesFactory.getGCRF();

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * 2nd part's name
     */
    private final String solarGenerator = "GS";
    /**
     * 3rd part's name
     */
    private final String axis1 = "axis1";

    /**
     * 4th part's name
     */
    private final String axis2 = "axis2";

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(InertiaModelTest.class.getSimpleName(), "Inertia model");
    }

    @Before
    public void setUp() {
        Utils.clear();
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}
     * 
     * @testedMethod {@link InertiaSimpleModel#getInertiaMatrix(Frame, AbsoluteDate)}
     * @testedMethod {@link InertiaSimpleModel#getMass()}
     * @testedMethod {@link InertiaSimpleModel#getMassCenter(Frame, AbsoluteDate)}
     * @testedMethod {@link InertiaSimpleModel#updateIntertiaMatrix(Matrix3D)}
     * @testedMethod {@link InertiaSimpleModel#updateMass(double)}
     * @testedMethod {@link InertiaSimpleModel#updateMassCenter(Vector3D)}
     * 
     * @description creation of a simple inertia model and check of the returned values
     * 
     * @input the mass, mass center and inertia matrix
     * 
     * @output the mass, mass center and inertia matrix
     * 
     * @testPassCriteria the returned values are the given ones
     * 
     * @see InertiaSimpleModel
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void simpleInertiaTest() throws PatriusException {

        // model creation
        Vector3D massCenter = new Vector3D(1.0, 2.0, 3.0);
        final double[][] dataIn = { { 7.28, -0.697, 0.404 }, { -0.697, 10.1, 0.121 }, { 0.404, 0.121, 10.3 } };
        Matrix3D inertiaMatrix = new Matrix3D(dataIn);

        final String name = "default";

        InertiaSimpleModel model = new InertiaSimpleModel(50.0, massCenter, inertiaMatrix, this.refFrame, name);

        Assert.assertEquals(50.0, model.getTotalMass(), this.comparisonEpsilon);
        Assert.assertEquals(1.0, model.getMassCenter(this.refFrame, this.date).getX(), this.comparisonEpsilon);
        Assert.assertEquals(2.0, model.getMassCenter(this.refFrame, this.date).getY(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, model.getMassCenter(this.refFrame, this.date).getZ(), this.comparisonEpsilon);

        Assert.assertTrue(inertiaMatrix.equals(model.getInertiaMatrix(this.refFrame, this.date)));

        massCenter = new Vector3D(7.0, 3.0, -5.0);
        model.updateMassCenter(massCenter);
        Assert.assertEquals(7.0, model.getMassCenter(this.refFrame, this.date).getX(), this.comparisonEpsilon);
        Assert.assertEquals(3.0, model.getMassCenter(this.refFrame, this.date).getY(), this.comparisonEpsilon);
        Assert.assertEquals(-5.0, model.getMassCenter(this.refFrame, this.date).getZ(), this.comparisonEpsilon);

        model.updateMass(name, 85.0);
        Assert.assertEquals(85.0, model.getTotalMass(), this.comparisonEpsilon);

        inertiaMatrix = inertiaMatrix.multiply(-9);
        model.updateIntertiaMatrix(inertiaMatrix);
        Assert.assertTrue(inertiaMatrix.equals(model.getInertiaMatrix(this.refFrame, this.date)));

        // test in another frame
        // frame creation
        final Vector3D translation = new Vector3D(0.115, -0.2908, 0.0);
        final Rotation rotation = new Rotation(RotationOrder.ZXZ, 5. * MathUtils.DEG_TO_RAD,
            10. * MathUtils.DEG_TO_RAD, 15. * MathUtils.DEG_TO_RAD);
        final Transform transformRot = new Transform(AbsoluteDate.J2000_EPOCH, rotation);
        final Transform transformTrans = new Transform(AbsoluteDate.J2000_EPOCH, translation);
        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, transformTrans, transformRot);
        final Frame secondFrame = new Frame(this.refFrame, transform, "secondFrame");

        // matrix test
        model.updateIntertiaMatrix(new Matrix3D(dataIn));
        final double[][] expectedMat = { { 7.2032254, 0.4483698, 0.4921355 },
            { 0.4483698, 10.208986, -0.0310831 },
            { 0.4921355, -0.0310831, 10.267789 } };
        final double[][] resultMat = model.getInertiaMatrix(secondFrame, this.date).getData();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(expectedMat[i][j], resultMat[i][j], 1.0e-6);
            }
        }

        // mass center test
        final Vector3D expectedVector = transform.transformPosition(massCenter);
        final Vector3D resultVect = model.getMassCenter(secondFrame, this.date);
        Assert.assertEquals(expectedVector.getX(), resultVect.getX(), 1.0e-7);
        Assert.assertEquals(expectedVector.getY(), resultVect.getY(), 1.0e-7);
        Assert.assertEquals(expectedVector.getZ(), resultVect.getZ(), 1.0e-7);

        // test Huygens' theorem when constructing the inertia model:
        massCenter = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D inertiaMatrixCenter = new Vector3D(1.0, 3.0, 2.0);
        final double[][] inertia = { { 1.0, 4.0, -2.0 }, { 4.0, 3.0, 0.0 }, { -2.0, 0.0, 3.0 } };
        inertiaMatrix = new Matrix3D(inertia);
        model = new InertiaSimpleModel(1.5, massCenter, inertiaMatrix, inertiaMatrixCenter, this.refFrame, name);
        final Matrix3D actual = model.getInertiaMatrix(this.refFrame, this.date);
        Assert.assertEquals(1.0 - 1.5 * 13, actual.getEntry(0, 0), 0.0);
        Assert.assertEquals(3.0 - 1.5 * 5, actual.getEntry(1, 1), 0.0);
        Assert.assertEquals(3.0 - 1.5 * 10, actual.getEntry(2, 2), 0.0);
        Assert.assertEquals(4.0 + 1.5 * 3, actual.getEntry(0, 1), 0.0);
        Assert.assertEquals(-2.0 + 1.5 * 2, actual.getEntry(2, 0), 0.0);

        // test Huygens' theorem when getting the inertia matrix
        final double[][] resultMat2 = model.getInertiaMatrix(this.refFrame, this.date, inertiaMatrixCenter).getData();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(inertia[i][j], resultMat2[i][j], 1.0e-6);
            }
        }
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}
     * 
     * @testedMethod {@link InertiaSimpleModel#getMass()}
     * @testedMethod {@link InertiaSimpleModel#addMassDerivative()}
     * @testedMethod {@link InertiaSimpleModel#getAdditionalEquation}
     * @testedMethod {@link InertiaSimpleModel#getAllPartsNames()}
     * 
     * @description creation of a simple inertia model and check of the returned values
     * 
     * @input the mass, mass center and inertia matrix
     * 
     * @output the mass, mass center and inertia matrix
     * 
     * @testPassCriteria the returned values are the given ones
     * 
     * @see InertiaSimpleModel
     * 
     * @since 2.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void simpleModelCoverageTest() throws PatriusException {

        // model creation
        final Vector3D massCenter = new Vector3D(1.0, 2.0, 3.0);
        final double[][] dataIn = { { 7.28, -0.697, 0.404 }, { -0.697, 10.1, 0.121 }, { 0.404, 0.121, 10.3 } };
        final Matrix3D inertiaMatrix = new Matrix3D(dataIn);

        final String name = "default";

        final InertiaSimpleModel model = new InertiaSimpleModel(50.0, massCenter, inertiaMatrix, this.refFrame, name);

        Assert.assertEquals(50.0, model.getMass(name), this.comparisonEpsilon);

        final List<String> liste = model.getAllPartsNames();
        Assert.assertEquals(liste.get(0), name);

        model.addMassDerivative(name, -1);
        model.getAdditionalEquation(name);

        // exception should occur :
        model.getMass("This is not a part name");
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_COMPUTED_MODEL}
     * 
     * @testedMethod {@link InertiaComputedModel#getInertiaMatrix(Frame, AbsoluteDate)}
     * @testedMethod {@link InertiaComputedModel#getMass()}
     * @testedMethod {@link InertiaComputedModel#getMassCenter(Frame, AbsoluteDate)}
     * 
     * @description creation of a "computed" inertia model from
     *              an assembly and check of the returned computed values
     * 
     * @input the assembly with some MASS and INERTIA properties
     * 
     * @output the mass, mass center and inertia matrix
     * 
     * @testPassCriteria the returned values are the given ones
     * 
     * @see InertiaComputedModel
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void computedInertiaTest() throws PatriusException {

        Report.printMethodHeader("computedInertiaTest", "Inertia computation", "Unknwown",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // mass properties
        final MassProperty massMainBody = new MassProperty(116.473);
        final MassProperty massGS = new MassProperty(6.440);
        final MassProperty massAxis1 = new MassProperty(0.826);
        final MassProperty massAxis2 = new MassProperty(0.826);

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);
            builder.addProperty(massMainBody, this.mainBody);

            // add other parts
            // GS
            final Vector3D translationGS = new Vector3D(0.115, -0.2908, 0.0);
            final Vector3D rotAxisGS = Vector3D.PLUS_K;
            final Rotation rotationGS = new Rotation(rotAxisGS, -(FastMath.PI + 13.0 * MathUtils.DEG_TO_RAD));
            builder.addPart(this.solarGenerator, this.mainBody, translationGS, rotationGS);
            builder.addProperty(massGS, this.solarGenerator);

            // axis 1
            final Vector3D translationAxis1 = new Vector3D(0.7061, -0.0666, -0.2017);
            builder.addPart(this.axis1, this.mainBody, translationAxis1, Rotation.IDENTITY);
            builder.addProperty(massAxis1, this.axis1);

            // axis 2
            final Vector3D translationAxis2 = new Vector3D(0.6323, -0.08626, 0.120670);
            builder.addPart(this.axis2, this.mainBody, translationAxis2, Rotation.IDENTITY);
            builder.addProperty(massAxis2, this.axis2);

            // inertia properties
            // main Part
            Vector3D massCenter = new Vector3D(0.3352, -1.2700E-03, -9.10E-03);
            final double[][] dataIn1 = { { 7.28, -0.697, 0.404 }, { -0.697, 10.1, 0.121 }, { 0.404, 0.121, 10.3 } };
            Matrix3D inertiaMatrix = new Matrix3D(dataIn1);
            final InertiaSimpleProperty inertia1 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massMainBody);
            inertia1.setMassCenter(massCenter);
            inertia1.setInertiaMatrix(inertiaMatrix);
            builder.addProperty(inertia1, this.mainBody);

            // GS
            massCenter = new Vector3D(0.0, 0.7849, 0.0016);
            final double[][] dataIn2 = { { 9.2814945, 0., 0. },
                { 0., 0.1422165, -0.0160876 },
                { 0., -0.0160876, 9.148478 } };
            inertiaMatrix = new Matrix3D(dataIn2);
            final InertiaSimpleProperty inertia2 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massGS);
            builder.addProperty(inertia2, this.solarGenerator);

            // axis 1
            massCenter = new Vector3D(165.8E-03, -2371.6E-03, 0.0);
            final double[][] dataIn3 = { { 10.769826, 0.7529925, 0. },
                { 0.7529925, 0.0530464, 0. },
                { 0., 0., 10.822532 } };
            inertiaMatrix = new Matrix3D(dataIn3);
            final InertiaSimpleProperty inertia3 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massAxis1);
            builder.addProperty(inertia3, this.axis1);

            // axis 2
            massCenter = new Vector3D(1681.1E-03, 117.27E-03, -1676.9E-03);
            final double[][] dataIn4 = { { 5.4110661, -0.3774398, 5.3975242 },
                { -0.3774398, 10.796063, 0.3765330 },
                { 5.3975242, 0.3765330, 5.4377157 } };
            inertiaMatrix = new Matrix3D(dataIn4);
            final InertiaSimpleProperty inertia4 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massAxis2);
            builder.addProperty(inertia4, this.axis2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // model
        final IInertiaModel model = new InertiaComputedModel(assembly);

        // tests
        Frame mainFrame = assembly.getPart(this.mainBody).getFrame();

        // mass test
        Assert.assertEquals(124.565, model.getTotalMass(), this.comparisonEpsilon);

        // matrix test
        final double[][] expectedMat = { { 45.719804, -3.9573377, 8.4511139 },
            { -3.9573377, 28.02953, 0.3358129 },
            { 8.4511139, 0.3358129, 51.637398 } };
        final double[][] resultMat = model.getInertiaMatrix(mainFrame, this.date).getData();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(expectedMat[i][j], resultMat[i][j], 1.0e-6);
            }
        }

        Report.printToReport("Inertia matrix", expectedMat, resultMat);

        // mass center test
        final UpdatableFrame mainpartFrame = new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY,
            "yellow submarine");
        assembly.initMainPartFrame(mainpartFrame);
        mainFrame = assembly.getPart(this.mainBody).getFrame();
        final Vector3D expectedVector = new Vector3D(0.3313638, -0.0717233, -0.0200831);
        final Vector3D resultVect = model.getMassCenter(mainFrame, this.date);
        Assert.assertEquals(expectedVector.getX(), resultVect.getX(), 1.0e-7);
        Assert.assertEquals(expectedVector.getY(), resultVect.getY(), 1.0e-7);
        Assert.assertEquals(expectedVector.getZ(), resultVect.getZ(), 1.0e-7);

        // test Huygens' theorem when constructing the inertia model:
        final Vector3D massCenter = new Vector3D(0.0, 0.0, 0.0);
        final Vector3D inertiaMatrixCenter = new Vector3D(1.0, 3.0, 2.0);
        final double[][] inertia = { { 1.0, 4.0, -2.0 }, { 4.0, 3.0, 0.0 }, { -2.0, 0.0, 3.0 } };
        final Matrix3D inertiaMatrix = new Matrix3D(inertia);
        final MassProperty mass = new MassProperty(1.5);

        final MainPart part = new MainPart("part");
        part.setFrame(mainpartFrame);
        final InertiaSimpleProperty property = new InertiaSimpleProperty(massCenter, inertiaMatrix,
            inertiaMatrixCenter, mass);
        final Matrix3D actual = property.getInertiaMatrix();
        Assert.assertEquals(1.0 - 1.5 * 13, actual.getEntry(0, 0), 0.0);
        Assert.assertEquals(3.0 - 1.5 * 5, actual.getEntry(1, 1), 0.0);
        Assert.assertEquals(3.0 - 1.5 * 10, actual.getEntry(2, 2), 0.0);
        Assert.assertEquals(4.0 + 1.5 * 3, actual.getEntry(0, 1), 0.0);
        Assert.assertEquals(-2.0 + 1.5 * 2, actual.getEntry(2, 0), 0.0);

        // test Huygens' theorem when getting the inertia matrix
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        try {
            // add main part
            builder2.addMainPart("part1");
            builder2.addProperty(property, "part1");
            builder2.addProperty(mass, "part1");
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
        final Assembly assembly2 = builder2.returnAssembly();
        final IInertiaModel model2 = new InertiaComputedModel(assembly2);
        final Frame resFrame = assembly2.getPart("part1").getFrame();
        final double[][] resultMat2 = model2.getInertiaMatrix(resFrame, this.date, inertiaMatrixCenter).getData();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(inertia[i][j], resultMat2[i][j], 1.0e-6);
            }
        }
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}
     * 
     * @testedMethod {@link InertiaComputedModel#checkProperty()}
     * 
     * @description error case for checkProperty
     * 
     * @testPassCriteria IllegalArgumentException
     * 
     * @see InertiaSimpleModel
     * 
     * @since 2.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void computedInertiaCoverageTest() throws PatriusException {
        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // mass properties
        final MassProperty massMainBody = new MassProperty(116.473);
        final MassProperty massGS = new MassProperty(6.440);
        final MassProperty massAxis1 = new MassProperty(0.826);
        final MassProperty massAxis2 = new MassProperty(0.826);

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);
            builder.addProperty(massMainBody, this.mainBody);

            // add other parts
            // GS
            final Vector3D translationGS = new Vector3D(0.115, -0.2908, 0.0);
            final Vector3D rotAxisGS = Vector3D.PLUS_K;
            final Rotation rotationGS = new Rotation(rotAxisGS, -(FastMath.PI + 13.0 * MathUtils.DEG_TO_RAD));
            builder.addPart(this.solarGenerator, this.mainBody, translationGS, rotationGS);
            builder.addProperty(massGS, this.solarGenerator);

            // axis 1
            final Vector3D translationAxis1 = new Vector3D(0.7061, -0.0666, -0.2017);
            builder.addPart(this.axis1, this.mainBody, translationAxis1, Rotation.IDENTITY);
            builder.addProperty(massAxis1, this.axis1);

            // axis 2
            final Vector3D translationAxis2 = new Vector3D(0.6323, -0.08626, 0.120670);
            builder.addPart(this.axis2, this.mainBody, translationAxis2, Rotation.IDENTITY);
            builder.addProperty(massAxis2, this.axis2);

            // inertia properties
            // main Part
            Vector3D massCenter = new Vector3D(0.3352, -1.2700E-03, -9.10E-03);
            final double[][] dataIn1 = { { 7.28, -0.697, 0.404 }, { -0.697, 10.1, 0.121 }, { 0.404, 0.121, 10.3 } };
            Matrix3D inertiaMatrix = new Matrix3D(dataIn1);
            final InertiaSimpleProperty inertia1 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massMainBody);
            inertia1.setMassCenter(massCenter);
            inertia1.setInertiaMatrix(inertiaMatrix);
            builder.addProperty(inertia1, this.mainBody);

            // GS
            massCenter = new Vector3D(0.0, 0.7849, 0.0016);
            final double[][] dataIn2 = { { 9.2814945, 0., 0. },
                { 0., 0.1422165, -0.0160876 },
                { 0., -0.0160876, 9.148478 } };
            inertiaMatrix = new Matrix3D(dataIn2);
            final InertiaSimpleProperty inertia2 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massGS);
            builder.addProperty(inertia2, this.solarGenerator);

            // axis 1
            massCenter = new Vector3D(165.8E-03, -2371.6E-03, 0.0);
            final double[][] dataIn3 = { { 10.769826, 0.7529925, 0. },
                { 0.7529925, 0.0530464, 0. },
                { 0., 0., 10.822532 } };
            inertiaMatrix = new Matrix3D(dataIn3);
            final InertiaSimpleProperty inertia3 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massAxis1);
            builder.addProperty(inertia3, this.axis1);

            // axis 2
            massCenter = new Vector3D(1681.1E-03, 117.27E-03, -1676.9E-03);
            final double[][] dataIn4 = { { 5.4110661, -0.3774398, 5.3975242 },
                { -0.3774398, 10.796063, 0.3765330 },
                { 5.3975242, 0.3765330, 5.4377157 } };
            inertiaMatrix = new Matrix3D(dataIn4);
            final InertiaSimpleProperty inertia4 =
                new InertiaSimpleProperty(massCenter, inertiaMatrix, massAxis2);
            builder.addProperty(inertia4, this.axis2);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // model
        final IInertiaModel model = new InertiaComputedModel(assembly);

        // exception should occur :
        model.getMass("This is not a part name");
    }

    /**
     * @throws PatriusException
     *         in case of a frame error
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}, {@link features#INERTIA_COMPUTED_MODEL}
     * 
     * @testedMethod {@link InertiaSimpleModel#setMassDerivativeZero()}
     * @testedMethod {@link InertiaComputedModel#setMassDerivativeZero()}
     * 
     * @description check mass derivative is properly set to 0 in the corresponding equation
     * 
     * @testPassCriteria mass derivative value is 0
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void setMassDerivativeZeroTest() throws PatriusException {

        // Models creation
        final Vector3D massCenter = new Vector3D(1.0, 2.0, 3.0);
        final double[][] dataIn = { { 7.28, -0.697, 0.404 }, { -0.697, 10.1, 0.121 }, { 0.404, 0.121, 10.3 } };
        final Matrix3D inertiaMatrix = new Matrix3D(dataIn);
        final String partName = "tank";

        final InertiaSimpleModel model1 =
            new InertiaSimpleModel(50.0, massCenter, inertiaMatrix, this.refFrame, partName);

        final AssemblyBuilder builder = new AssemblyBuilder();
        final InertiaSimpleProperty massMainBody = new InertiaSimpleProperty(massCenter, inertiaMatrix,
            new MassProperty(50.));
        final InertiaSimpleProperty massGS = new InertiaSimpleProperty(massCenter, inertiaMatrix, new MassProperty(6.));
        builder.addMainPart(this.mainBody);
        builder.addProperty(massMainBody, this.mainBody);
        builder.addPart(this.solarGenerator, this.mainBody, Vector3D.ZERO, Rotation.IDENTITY);
        builder.addProperty(massGS, this.solarGenerator);

        final InertiaComputedModel model2 = new InertiaComputedModel(builder.returnAssembly());

        // ================== Set mass derivative to -5, then to zero ==================

        // Initialization
        final AccelerationRetriever retriever = new AccelerationRetriever();

        // Model 1
        SpacecraftState state1 = new SpacecraftState(null);
        state1 = state1.addAdditionalState("MASS_" + partName, new double[] { 10. });

        model1.addMassDerivative(partName, -5);
        model1.getAdditionalEquation(partName).computeDerivatives(state1, retriever);
        Assert.assertEquals(retriever.getDerivative(), -5, 0.);

        model1.setMassDerivativeZero(partName);
        model1.getAdditionalEquation(partName).computeDerivatives(state1, retriever);
        Assert.assertEquals(retriever.getDerivative(), 0., 0.);

        // Model 2
        SpacecraftState state2 = new SpacecraftState(null);
        state2 = state2.addAdditionalState("MASS_" + this.mainBody, new double[] { 50. });
        state2 = state2.addAdditionalState("MASS_" + this.solarGenerator, new double[] { 6. });

        model2.addMassDerivative(this.solarGenerator, -5);
        model2.getAdditionalEquation(this.solarGenerator).computeDerivatives(state2, retriever);
        Assert.assertEquals(retriever.getDerivative(), -5, 0.);

        model2.setMassDerivativeZero(this.solarGenerator);
        model2.getAdditionalEquation(this.solarGenerator).computeDerivatives(state2, retriever);
        Assert.assertEquals(retriever.getDerivative(), 0., 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INERTIA_SIMPLE_MODEL}
     * 
     * @testedMethod {@link InertiaSimpleModel#getTotalMass(SpacecraftState)}
     * @testedMethod {@link InertiaComputedModel#getTotalMass(SpacecraftState)}
     * 
     * @description check the mass is the expected one.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetTotalMassSpacecraftState() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);

        // Simple inertia model case

        final MassProvider massModel1 = new InertiaSimpleModel(1000, Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I),
            FramesFactory.getEME2000(), "Main");
        final MassProvider massModel2 = new InertiaSimpleModel(900, Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I),
            FramesFactory.getEME2000(), "Main");

        // Check without mass provider
        Assert.assertEquals(1000., massModel1.getTotalMass(new SpacecraftState(orbit)), 0.);

        // Check with mass provider
        Assert.assertEquals(900., massModel1.getTotalMass(new SpacecraftState(orbit, massModel2)), 0.);

        // Computed inertia model case

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addPart("Part1", "Main", Transform.IDENTITY);
        builder.addPart("Part2", "Main", Transform.IDENTITY);
        builder.addProperty(new InertiaSimpleProperty(Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I), new MassProperty(
            1500.)), "Part1");
        builder.addProperty(new InertiaSimpleProperty(Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I), new MassProperty(
            1400.)), "Part2");
        final MassProvider massModel3 = new InertiaComputedModel(builder.returnAssembly());
        builder.addMainPart("Main2");
        builder.addPart("Part1", "Main2", Transform.IDENTITY);
        builder.addPart("Part2", "Main2", Transform.IDENTITY);
        builder.addProperty(new InertiaSimpleProperty(Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I), new MassProperty(
            1500.)), "Part1");
        builder.addProperty(new InertiaSimpleProperty(Vector3D.ZERO, new Matrix3D(Vector3D.PLUS_I), new MassProperty(
            1200.)), "Part2");
        final MassProvider massModel4 = new InertiaComputedModel(builder.returnAssembly());

        // Check without mass provider
        Assert.assertEquals(2900., massModel3.getTotalMass(new SpacecraftState(orbit)), 0.);

        // Check with mass provider
        Assert.assertEquals(2700., massModel3.getTotalMass(new SpacecraftState(orbit, massModel4)), 0.);
    }

    /**
     * Acceleration retriever for testing purpose.
     */
    private static class AccelerationRetriever implements TimeDerivativesEquations {

        private static final long serialVersionUID = -4616792058307814184L;
        private double derivative;

        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {
            // nothing to do
        }

        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            // nothing to do
        }

        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame) {
            // nothing to do
        }

        public double getDerivative() {
            return this.derivative;
        }

        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            this.derivative = pDot[0];
        }
    }
}
