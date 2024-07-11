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
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.vehicle;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AeroCoeffByAltitude;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AeroCoeffByAoA;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AeroCoeffConstant;
import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AerodynamicCoefficientType;
import fr.cnes.sirius.patrius.assembly.properties.AeroGlobalProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeIRProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightCircularCylinder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RightParallelepiped;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class VehicleTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Vehicle
         * 
         * @featureDescription Vehicle
         * 
         * @coveredRequirements
         */
        VEHICLE,
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICLE}
     * 
     * @testedMethod all {@link VehicleSurfaceModel} methods
     * 
     * @description check that the VehicleSurfaceModel methods are correct
     * 
     * @input VehicleSurfaceModel
     * 
     * @output output of VehicleSurfaceModel methods
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void vehicleSurfaceModelTest() throws PatriusException {
        // Initialization
        final CrossSectionProvider shape1 = new Sphere(Vector3D.ZERO, 2.);
        final CrossSectionProvider shape2 = new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_I, 2., 3.);
        final CrossSectionProvider shape3 = new RightParallelepiped(2., 3., 5.);
        final RightParallelepiped solarPanels = new RightParallelepiped(1., 2., 3.);
        final VehicleSurfaceModel vehicle1 = new VehicleSurfaceModel(shape1);
        final VehicleSurfaceModel vehicle2 = new VehicleSurfaceModel(shape2, solarPanels);
        final VehicleSurfaceModel vehicle3 = new VehicleSurfaceModel(shape3, solarPanels, 3.5);

        // Check cross section
        Assert.assertEquals(0., vehicle1.getCrossSection(Vector3D.PLUS_I), FastMath.PI * 2. * 2.);
        Assert.assertEquals(0., vehicle2.getCrossSection(Vector3D.PLUS_I), FastMath.PI * 2. * 2. + 1.);
        Assert.assertEquals(0., vehicle3.getCrossSection(Vector3D.PLUS_I), 2. + 1.);

        // Other minor checks
        Assert.assertEquals(3.5, vehicle3.getMultiplicativeFactor().getValue(), 0.);
        vehicle3.setMultiplicativeFactor(4.5);
        Assert.assertEquals(4.5, vehicle3.getMultiplicativeFactor().getValue(), 0.);
        Assert.assertEquals(shape3, vehicle3.getMainPartShape());
        Assert.assertEquals(solarPanels, vehicle3.getSolarPanelsShape());
        Assert.assertTrue(vehicle3.equals(new VehicleSurfaceModel(shape3, solarPanels, 4.5)));
        Assert.assertFalse(vehicle3.equals(vehicle2));
        Assert.assertNotNull(vehicle3.toString());

        // Exceptions
        try {
            new VehicleSurfaceModel(null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICLE}
     * 
     * @testedMethod all {@link AerodynamicProperties} methods
     * 
     * @description check that the AerodynamicProperties methods are correct
     * 
     * @input VehicleSurfaceModel
     * 
     * @output output of AerodynamicProperties methods
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void aerodynamicPropertiesTest() throws PatriusException {

        // State
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Initialization
        final Sphere shape = new Sphere(Vector3D.ZERO, 2.);
        final AeroCoeffConstant cx = new AeroCoeffConstant(new Parameter("", 3.2));
        final AeroCoeffConstant cz = new AeroCoeffConstant(new Parameter("", 4.2));
        final AeroCoeffByAoA cx2 = new AeroCoeffByAoA(new double[] { 1E5, 1E6 }, new double[] { 2.2, 2.3 }, null);
        final AeroCoeffByAltitude cz2 =
            new AeroCoeffByAltitude(new double[] { 1E5, 1E6 }, new double[] { 2.2, 2.3 }, null);
        final VehicleSurfaceModel vehicle = new VehicleSurfaceModel(shape);
        final AerodynamicProperties aeroProp1 = new AerodynamicProperties(shape, cx.getAerodynamicCoefficient());
        final AerodynamicProperties aeroProp2 = new AerodynamicProperties(shape, cx2, cz2);
        final AerodynamicProperties aeroProp3 =
            new AerodynamicProperties(vehicle, cx.getAerodynamicCoefficient(), cz.getAerodynamicCoefficient());
        final AerodynamicProperties aeroProp4 = new AerodynamicProperties(shape, cx2, cx2);

        // Check cross section
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        final AssemblyBuilder builder3 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder2.addMainPart("Main");
        builder3.addMainPart("Main");
        aeroProp1.setAerodynamicProperties(builder1, "Main", 2.);
        aeroProp2.setAerodynamicProperties(builder2, "Main", 3.);
        aeroProp3.setAerodynamicProperties(builder3, "Main", 4.);
        final Assembly assembly1 = builder1.returnAssembly();
        final Assembly assembly2 = builder2.returnAssembly();
        final Assembly assembly3 = builder3.returnAssembly();
        Assert.assertEquals(FastMath.PI * 2. * 2. * 2.,
            ((AeroSphereProperty) assembly1.getMainPart().getProperty(PropertyType.AERO_CROSS_SECTION))
                .getCrossSection(state, Vector3D.PLUS_I, assembly1.getMainPart().getFrame(), assembly1.getMainPart()
                    .getFrame()), 1E-14);
        Assert.assertEquals(FastMath.PI * 2. * 2. * 3.,
            ((AeroGlobalProperty) assembly2.getMainPart().getProperty(PropertyType.AERO_GLOBAL))
                .getCrossSection(Vector3D.PLUS_I), 1E-14);
        Assert.assertEquals(FastMath.PI * 2. * 2. * 4.,
            ((AeroGlobalProperty) assembly3.getMainPart().getProperty(PropertyType.AERO_GLOBAL))
                .getCrossSection(Vector3D.PLUS_I), 1E-14);

        // Other minor checks
        Assert.assertEquals(AerodynamicCoefficientType.CONSTANT, aeroProp1.getFunctionType());
        Assert.assertEquals(AerodynamicCoefficientType.CONSTANT, aeroProp1.getDragCoef().getType());
        Assert.assertEquals(AerodynamicCoefficientType.CONSTANT, aeroProp1.getLiftCoef().getType());
        Assert.assertEquals(cx.getAerodynamicCoefficient(), aeroProp3.getConstantDragCoef());
        Assert.assertEquals(cz.getAerodynamicCoefficient(), aeroProp3.getConstantLiftCoef());
        Assert.assertNotNull(aeroProp3.toString());
        Assert.assertNotNull(aeroProp4.toString());

        // Exceptions
        try {
            aeroProp2.getFunctionType();
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            aeroProp2.getConstantDragCoef();
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            aeroProp2.getConstantLiftCoef();
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new AerodynamicProperties(null, 2.2, 3.2);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new AerodynamicProperties(shape, cx, null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new AerodynamicProperties(shape, null, cz);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VEHICLE}
     * 
     * @testedMethod all {@link RadiativeProperties} methods
     * 
     * @description check that the RadiativeProperties methods are correct
     * 
     * @input VehicleSurfaceModel
     * 
     * @output output of RadiativeProperties methods
     * 
     * @testPassCriteria result is as expected (reference computed mathematically, threshold: 1E-14)
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void radiativePropertiesTest() throws PatriusException {

        // State
        final Orbit orbit =
            new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH, Constants.GRIM5C1_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Initialization
        final RadiativeProperty radiativeProperty = new RadiativeProperty(0.1, 0.2, 0.7);
        final RadiativeIRProperty radiativeIRProperty = new RadiativeIRProperty(0.5, 0.2, 0.3);
        final VehicleSurfaceModel vehicle1 = new VehicleSurfaceModel(new Sphere(Vector3D.ZERO, 2.));
        final VehicleSurfaceModel vehicle2 =
            new VehicleSurfaceModel(new RightParallelepiped(1, 2, 3), new RightParallelepiped(1, 2, 3));
        final VehicleSurfaceModel vehicle3 =
            new VehicleSurfaceModel(new RightCircularCylinder(Vector3D.ZERO, Vector3D.PLUS_I, 2., 3.),
                new RightParallelepiped(1, 2, 3));
        final RadiativeProperties radiativeProp1 =
            new RadiativeProperties(radiativeProperty, radiativeIRProperty, vehicle1);
        final RadiativeProperties radiativeProp2 =
            new RadiativeProperties(radiativeProperty, radiativeIRProperty, vehicle2);
        final RadiativeProperties radiativeProp3 =
            new RadiativeProperties(radiativeProperty, radiativeIRProperty, vehicle3);

        // Check radiative property
        final AssemblyBuilder builder1 = new AssemblyBuilder();
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        final AssemblyBuilder builder3 = new AssemblyBuilder();
        builder1.addMainPart("Main");
        builder2.addMainPart("Main");
        builder3.addMainPart("Main");
        radiativeProp1.setRadiativeProperties(builder1, "Main", 2.);
        radiativeProp2.setRadiativeProperties(builder2, "Main", 3.);
        radiativeProp3.setRadiativeProperties(builder3, "Main", 4.);
        final Assembly assembly1 = builder1.returnAssembly();
        final Assembly assembly2 = builder2.returnAssembly();
        final Assembly assembly3 = builder3.returnAssembly();
        Assert.assertEquals(0.1, ((RadiativeProperty) assembly1.getMainPart().getProperty(PropertyType.RADIATIVE))
            .getAbsorptionRatio().getValue(), 1E-14);
        Assert.assertEquals(0.5, ((RadiativeIRProperty) assembly1.getMainPart().getProperty(PropertyType.RADIATIVEIR))
            .getAbsorptionCoef().getValue(), 1E-14);
        Assert.assertEquals(FastMath.PI * 2. * 2. * 2.,
            ((RadiativeSphereProperty) assembly1.getMainPart().getProperty(PropertyType.RADIATIVE_CROSS_SECTION))
                .getCrossSection(state, Vector3D.PLUS_I, assembly1.getMainPart().getFrame(), assembly1.getMainPart()
                    .getFrame()), 1E-14);
        Assert.assertEquals(0.1,
            ((RadiativeProperty) assembly2.getPart("surf{1; 0; 0}").getProperty(PropertyType.RADIATIVE))
                .getAbsorptionRatio().getValue(), 1E-14);
        Assert.assertEquals(0.5,
            ((RadiativeIRProperty) assembly2.getPart("surf{1; 0; 0}").getProperty(PropertyType.RADIATIVEIR))
                .getAbsorptionCoef().getValue(), 1E-14);
        Assert.assertEquals(1. * 3.,
            ((RadiativeFacetProperty) assembly2.getPart("surf{1; 0; 0}").getProperty(PropertyType.RADIATIVE_FACET))
                .getFacet().getCrossSection(Vector3D.MINUS_I), 1E-14);
        Assert.assertEquals(0.1,
            ((RadiativeProperty) assembly3.getPart("surf{1; 0; 0}").getProperty(PropertyType.RADIATIVE))
                .getAbsorptionRatio().getValue(), 1E-14);
        Assert.assertEquals(0.5,
            ((RadiativeIRProperty) assembly3.getPart("surf{1; 0; 0}").getProperty(PropertyType.RADIATIVEIR))
                .getAbsorptionCoef().getValue(), 1E-14);
        Assert.assertEquals(FastMath.PI * 2. * 2. * 4., ((RadiativeFacetProperty) assembly3.getPart("surf{1; 0; 0}")
            .getProperty(PropertyType.RADIATIVE_FACET)).getFacet().getCrossSection(Vector3D.MINUS_I), 1E-14);

        // Other minor checks
        Assert.assertEquals(0.1, radiativeProp1.getRadiativeProperty().getAbsorptionRatio().getValue(), 0.);
        Assert.assertEquals(0.5, radiativeProp1.getRadiativeIRProperty().getAbsorptionCoef().getValue(), 0.);
        Assert.assertEquals(FastMath.PI * 2. * 2.,
            radiativeProp1.getVehicleSurfaceModel().getCrossSection(Vector3D.PLUS_I));
        Assert.assertNotNull(radiativeProp1.toString());

        // Exceptions
        try {
            new RadiativeProperties(null, radiativeIRProperty, vehicle1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new RadiativeProperties(new RadiativeProperty(0.1, 0.2, 0.6), radiativeIRProperty, vehicle1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new RadiativeProperties(radiativeProperty, new RadiativeIRProperty(0.5, 0.2, 0.4), vehicle1);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
        try {
            new RadiativeProperties(radiativeProperty, radiativeIRProperty, null);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }
}
