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
 * @history creation 22/07/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:22/07/2013:Created the solar radiation wrench sensitive assembly model
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1192:30/08/2017:update parts frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeApplicationPoint;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.wrenches.Wrench;

/**
 * @description <p>
 *              Test class for the radiation pressure model.
 *              </p>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class DirectRadiativeWrenchModelTest {

    /** Features description. */
    public enum features {
        /**
         * /**
         * 
         * @featureTitle Radiative model wrench
         * 
         * @featureDescription Computation of the radiation pressure wrench
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_50
         */
        RADIATIVE_MODEL
    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(DirectRadiativeWrenchModelTest.class.getSimpleName(), "Direct radiative wrench model");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeWrenchModel#DirectRadiativeWrenchModel(Assembly)}
     * 
     * @description Creation of an assembly and testing the radiative properties.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output exceptions
     * 
     * @testPassCriteria an IllegalArgumentException is thrown when building an instance of DirectRadiativeWrenchModel
     * 
     * @referenceVersion 2.1
     */
    @Test
    public void assemblyTest() {

        /**
         * Test on a model with a sphere and a facet with no mass property.
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // one facet
        final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
        builder.addProperty(radSphereProp, this.mainBody);
        final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
        builder.addProperty(radFacetProp, this.part2);

        // adding radiative properties
        final IPartProperty radProp = new RadiativeProperty(0.6, 0.4, 0.);
        builder.addProperty(radProp, this.mainBody);
        builder.addProperty(radProp, this.part2);

        // no mass property !

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // radiative model
        try {
            new DirectRadiativeWrenchModel(assembly);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }

        /**
         * Test on a model with a part with redundant radiative properties.
         */
        final AssemblyBuilder builder2 = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder2.addMainPart("Main2");
        builder2.addPart(this.part2, "Main2", Transform.IDENTITY);

        // one facet
        final Facet facet2 = new Facet(new Vector3D(0.0, 0.0, -2.0), 25 * FastMath.PI);

        final IPartProperty radSphereProp2 = new RadiativeSphereProperty(5.);
        builder2.addProperty(radSphereProp2, "Main2");
        final IPartProperty radFacetProp2 = new RadiativeFacetProperty(facet2);
        builder2.addProperty(radFacetProp2, this.part2);
        // redundant radiative property !
        builder2.addProperty(radSphereProp2, this.part2);

        // adding radiative properties
        final IPartProperty radMainProp2 = new RadiativeProperty(0.6, 0.4, 1.);
        builder2.addProperty(radMainProp2, "Main2");
        builder2.addProperty(radMainProp2, this.part2);

        // assembly creation
        final Assembly assembly2 = builder2.returnAssembly();

        // radiative model
        try {
            new DirectRadiativeWrenchModel(assembly2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected !
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DirectRadiativeWrenchModel#radiationWrench(SpacecraftState, Vector3D)}
     * @testedMethod {@link DirectRadiativeWrenchModel#radiationWrench(SpacecraftState, Vector3D, Vector3D, Frame)}
     * 
     * @description Creation of an assembly and testing the radiation pressure wrench.
     * 
     * @input Assembly with radiative properties.
     * 
     * @output the radiation pressure wrench
     * 
     * @testPassCriteria the computed wrench
     * 
     * @referenceVersion 2.1
     */
    @Test
    public void radiationPressureAccelerationTest() {

        Report.printMethodHeader("radiationPressureAccelerationTest", "Wrench computation", "Unknwown",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        /**
         * Test on a model with a sphere and one facet
         */

        final AssemblyBuilder builder = new AssemblyBuilder();

        try {

            // -------------------- ASSEMBLY RADIATIVE MODEL ---------------- //

            // add main part (one sphere) and part2 (one facet)
            builder.addMainPart(this.mainBody);
            builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

            // one facet
            final Vector3D normal = new Vector3D(0.0, 0.0, -2.0);
            final Facet facet = new Facet(normal, 25 * FastMath.PI);

            final IPartProperty radSphereProp = new RadiativeSphereProperty(5.);
            builder.addProperty(radSphereProp, this.mainBody);
            final IPartProperty radFacetProp = new RadiativeFacetProperty(facet);
            builder.addProperty(radFacetProp, this.part2);

            // adding radiative properties
            final IPartProperty radMainProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radMainProp, this.mainBody);
            final IPartProperty radPartProp = new RadiativeProperty(1., 0., 0.);
            builder.addProperty(radPartProp, this.part2);

            // application points
            final RadiativeApplicationPoint apS = new RadiativeApplicationPoint(Vector3D.MINUS_I.scalarMultiply(3));
            builder.addProperty(apS, this.mainBody);
            final RadiativeApplicationPoint apF = new RadiativeApplicationPoint(Vector3D.PLUS_J.scalarMultiply(-2));
            builder.addProperty(apF, this.part2);

            // inertia simple prop
            final MassProperty mp = new MassProperty(1500);
            final InertiaSimpleProperty prop = new InertiaSimpleProperty(Vector3D.PLUS_I, new Matrix3D(new double[][] {
                { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }), mp);
            builder.addProperty(prop, this.mainBody);

            // // adding mass property
            // final double part2Mass = 1000.;
            // final IPartProperty massFacetProp = new MassProperty(part2Mass);
            // builder.addProperty(massFacetProp, part2);

            // assembly creation
            final Assembly assembly = builder.returnAssembly();

            // -------------------- THE WRENCH MODEL --------------------------- //

            // radiative model
            final DirectRadiativeWrenchModel radiativeModel = new DirectRadiativeWrenchModel(assembly);

            // -------------------- A SPACECRAFTSTATE -------------------------- //

            // spacecraft
            final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
            // mu from grim4s4_gr model
            final double mu = 0.39860043770442e+15;
            // GCRF reference frame
            final Frame referenceFrame = FramesFactory.getGCRF();
            // pos-vel
            final Vector3D pos = new Vector3D(4.05228560172917172e+07, -1.17844795966431592e+07,
                -6.58338151580381091e+05);
            final Vector3D vel = new Vector3D(8.57448611492193891e+02, 2.94919910671677371e+03,
                -4.06888496702080431e+01);
            final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
            final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
            // creation of the spacecraft with no attitude
            final SpacecraftState spacecraftState = new SpacecraftState(orbit, new Attitude(date, referenceFrame,
                Rotation.IDENTITY, Vector3D.ZERO));
            assembly.initMainPartFrame(spacecraftState);

            // -------------------- VALIDATE ACCELERATION -------------------------- //

            // incoming flux
            final Vector3D flux = new Vector3D(-4.468859271520124E-6, 1.140023784464046E-6, 4.943990823337371E-7);

            // compute radiation pressure acceleration
            final Wrench computedWrench = radiativeModel.radiationWrench(spacecraftState, flux);
            final Vector3D computedAcc = computedWrench.getForce()
                .scalarMultiply(1 / 1500.);

            // radiative coefficients
            final double mainPartAbsCoef = 1;
            final double mainPartSpeCoef = 0;
            // kP = S*(1+4*(1-absCoef)*(1-speCoef)/9)
            final double kP = 25 * FastMath.PI * (1 + 4 * (1 - mainPartAbsCoef) * (1 - mainPartSpeCoef) / 9);

            final Vector3D sphereExpectedForce = new Vector3D(kP, flux);

            // expected force on the facet
            // mass
            // radiative coefficients
            final double part2AbsCoef = 1;
            final double part2SpeCoef = 0;
            // orientation of the facet
            final double orientation = Vector3D.dotProduct(flux.normalize(), normal.normalize());
            // computedFlux = -orientation*S*||flux||/mass
            final double computedFlux = -orientation * 25 * FastMath.PI * flux.getNorm();
            // acceleration along the flux direction
            // cF = computedFlux*(k_diff+k_abs)
            final double cF = computedFlux * part2AbsCoef;
            // acceleration along the normal direction
            // cN = 2*computedFlux*(k_spe*orientation-k_diff/3)
            final double cN = 2 * computedFlux * part2SpeCoef * orientation;

            final Vector3D facetExpectedForce = new Vector3D(cF, flux.normalize(), cN, normal.normalize());

            // expected radiation pressure acceleration on the assembly
            final Vector3D expectedAcc = new Vector3D(1.0 / (1500),
                sphereExpectedForce.add(facetExpectedForce));

            // Orekit model = (1 - Ka)(1 - Ks)
            // PBD SIRIUS model = Kd
            Assert.assertEquals(0.0, computedAcc.subtract(expectedAcc).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

            // ------------------------ VALIDATE WRENCH -------------------- //

            final Vector3D expectedTorque = new Vector3D(-8.277642288147849e-006, 1.594588736441943e-004,
                -4.425140233902411e-004);
            Assert.assertEquals(0.0, computedWrench.getTorque().subtract(expectedTorque).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            final Vector3D target = new Vector3D(-2, 0, 0);
            final Vector3D original = Vector3D.PLUS_I;

            // combination of the position and the attitude
            final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, spacecraftState.getAttitude()
                .getOrientation());
            final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, spacecraftState.getOrbit()
                .getPVCoordinates());
            final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation);

            // main part frame
            final Frame fr = new Frame(spacecraftState.getFrame(), transform, "mainPartFrame");

            final Vector3D expectedTorqueDisplaced = new Vector3D(-8.277642288147849e-006, 1.594588736441943e-004,
                -4.425140233902411e-004).add(original.subtract(target).crossProduct(computedWrench.getForce()));
            final Vector3D actualTorque = radiativeModel.radiationWrench(spacecraftState, flux, target, fr).getTorque();
            Assert.assertEquals(0.0, actualTorque.subtract(expectedTorqueDisplaced).getNorm(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            Report.printToReport("Torque", expectedTorqueDisplaced, actualTorque);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit data initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }
}
