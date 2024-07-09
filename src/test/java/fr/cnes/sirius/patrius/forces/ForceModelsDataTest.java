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
 */
/* HISTORY
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:767:24/04/2018: Creation of ForceModelsData to collect the models data for a force computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.Vehicle;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.DragLiftModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.models.RediffusedRadiativeModel;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.US76;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.BalminoAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.tides.OceanTides;
import fr.cnes.sirius.patrius.forces.gravity.tides.OceanTidesDataProvider;
import fr.cnes.sirius.patrius.forces.gravity.tides.TerrestrialTides;
import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.FES2004FormatReader;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsFactory;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.KnockeRiesModel;
import fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationPressure;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureEllipsoid;
import fr.cnes.sirius.patrius.forces.relativistic.CoriolisRelativisticEffect;
import fr.cnes.sirius.patrius.forces.relativistic.LenseThirringRelativisticEffect;
import fr.cnes.sirius.patrius.forces.relativistic.SchwarzschildRelativisticEffect;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ForceModelsDataTest {

    @Test
    public void testForceModelsData() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("regular-dataPBASE");

        final Assembly assembly = getAssembly();
        // Forces initialization
        // Earth potential attraction model
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider dataPotentialCoef = GravityFieldFactory.getPotentialProvider();
        final double[][] c = dataPotentialCoef.getC(80, 80, true);
        final double[][] s = dataPotentialCoef.getS(80, 80, true);
        final BalminoAttractionModel earthPotentialAttractionModel =
            new BalminoAttractionModel(FramesFactory.getITRF(),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU, c, s, 0, 0);
        // Solar activity
        final SolarActivityDataProvider solarActivityDataProvider = new ConstantSolarActivity(100, 7);
        // Solar radiation pressure ellipsoid
        final SolarRadiationPressureEllipsoid solarRadiationPressureEllipsoid = new SolarRadiationPressureEllipsoid(
            CelestialBodyFactory.getSun(), new ExtendedOneAxisEllipsoid(
                Constants.CNES_STELA_AE,
                Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getGCRF(), "earth"),
            new DirectRadiativeModel(assembly));
        // Rediffused radiation pressure
        final RediffusedRadiationPressure rediffusedRadiationPressure = new RediffusedRadiationPressure(
            CelestialBodyFactory.getSun(), FramesFactory.getGCRF(), 0, 0, new KnockeRiesModel(),
            new RediffusedRadiativeModel(true, true, 10, 10, assembly));
        // Attraction of third body
        final ThirdBodyAttraction moonThirdBodyAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
        final ThirdBodyAttraction sunThirdBodyAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        final ThirdBodyAttraction venusThirdBodyAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getVenus());
        final ThirdBodyAttraction marsThirdBodyAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getMars());
        final ThirdBodyAttraction jupiterThirdBodyAttraction =
            new ThirdBodyAttraction(CelestialBodyFactory.getJupiter());
        // Ocean tides
        OceanTidesCoefficientsFactory.addOceanTidesCoefficientsReader(new FES2004FormatReader("fes2004_gr"));
        final OceanTidesCoefficientsProvider provider = OceanTidesCoefficientsFactory.getCoefficientsProvider();
        final TidesStandard standard = TidesStandard.GINS2004;
        final OceanTidesDataProvider dataProvider = new OceanTidesDataProvider(provider, standard);
        final OceanTides oceanTides = new OceanTides(FramesFactory.getGCRF(), Constants.CNES_STELA_AE,
            Constants.CNES_STELA_MU, 0, 8, 8, 1, 2, false, dataProvider);
        // Terrestrial tides
        final TerrestrialTides terrestrialTides =
            new TerrestrialTides(FramesFactory.getGCRF(), Constants.CNES_STELA_AE,
                Constants.CNES_STELA_MU);
        // Drag force
        final DragForce dragForce = new DragForce(new US76(new OneAxisEllipsoid(Constants.CNES_STELA_AE,
            Constants.IERS92_EARTH_FLATTENING, FramesFactory.getGCRF())), new DragLiftModel(assembly));
        // Coriolis relativistic effect
        final CoriolisRelativisticEffect coriolisRelativisticEffect = new CoriolisRelativisticEffect(0,
            CelestialBodyFactory.getSun());
        // Lense thirring relativistic effect
        final LenseThirringRelativisticEffect lenseThirringRelativisticEffect = new LenseThirringRelativisticEffect(0,
            FramesFactory.getGCRF());
        // Schwarzschild relativistic effect
        final SchwarzschildRelativisticEffect schwarzschildRelativisticEffect = new SchwarzschildRelativisticEffect(0);

        // Complete list of force model
        final List<ForceModel> forceModelsListOriginal = new ArrayList<ForceModel>();
        forceModelsListOriginal.add(earthPotentialAttractionModel);
        forceModelsListOriginal.add(solarRadiationPressureEllipsoid);
        forceModelsListOriginal.add(rediffusedRadiationPressure);
        forceModelsListOriginal.add(moonThirdBodyAttraction);
        forceModelsListOriginal.add(sunThirdBodyAttraction);
        forceModelsListOriginal.add(venusThirdBodyAttraction);
        forceModelsListOriginal.add(marsThirdBodyAttraction);
        forceModelsListOriginal.add(jupiterThirdBodyAttraction);
        forceModelsListOriginal.add(oceanTides);
        forceModelsListOriginal.add(terrestrialTides);
        forceModelsListOriginal.add(dragForce);
        forceModelsListOriginal.add(coriolisRelativisticEffect);
        forceModelsListOriginal.add(lenseThirringRelativisticEffect);
        forceModelsListOriginal.add(schwarzschildRelativisticEffect);

        // Force models data
        final ForceModelsData data = new ForceModelsData(earthPotentialAttractionModel, solarActivityDataProvider,
            solarRadiationPressureEllipsoid, rediffusedRadiationPressure, moonThirdBodyAttraction,
            sunThirdBodyAttraction, venusThirdBodyAttraction, marsThirdBodyAttraction, jupiterThirdBodyAttraction,
            oceanTides, terrestrialTides, dragForce, coriolisRelativisticEffect, lenseThirringRelativisticEffect,
            schwarzschildRelativisticEffect);

        // Test the getters when the data are inform by the constructor
        Assert.assertEquals(earthPotentialAttractionModel, data.getEarthPotentialAttractionModel());
        Assert.assertEquals(solarActivityDataProvider, data.getSolarActivityDataProvider());
        Assert.assertEquals(solarRadiationPressureEllipsoid, data.getSolarRadiationPressureEllipsoid());
        Assert.assertEquals(rediffusedRadiationPressure, data.getRediffusedRadiationPressure());
        Assert.assertEquals(moonThirdBodyAttraction, data.getMoonThirdBodyAttraction());
        Assert.assertEquals(sunThirdBodyAttraction, data.getSunThirdBodyAttraction());
        Assert.assertEquals(venusThirdBodyAttraction, data.getVenusThirdBodyAttraction());
        Assert.assertEquals(marsThirdBodyAttraction, data.getMarsThirdBodyAttraction());
        Assert.assertEquals(jupiterThirdBodyAttraction, data.getJupiterThirdBodyAttraction());
        Assert.assertEquals(oceanTides, data.getOceanTides());
        Assert.assertEquals(terrestrialTides, data.getTerrestrialTides());
        Assert.assertEquals(dragForce, data.getDragForce());
        Assert.assertEquals(coriolisRelativisticEffect, data.getCoriolisRelativisticEffect());
        Assert.assertEquals(lenseThirringRelativisticEffect, data.getLenseThirringRelativisticEffect());
        Assert.assertEquals(schwarzschildRelativisticEffect, data.getSchwarzschildRelativisticEffect());
        final List<ForceModel> forceModelsList = data.getForceModelsList();

        // The activity data provider is not a force model
        Assert.assertEquals(14, forceModelsList.size());

        // Setters test
        final ForceModelsData data2 = new ForceModelsData();
        data2.setEarthPotentialAttractionModel(earthPotentialAttractionModel);
        data2.setDragForce(dragForce);
        data2.setJupiterThirdBodyAttraction(jupiterThirdBodyAttraction);
        data2.setMarsThirdBodyAttraction(marsThirdBodyAttraction);
        data2.setMoonThirdBodyAttraction(moonThirdBodyAttraction);
        data2.setOceanTides(oceanTides);
        data2.setRediffusedRadiationPressure(rediffusedRadiationPressure);
        data2.setSolarActivityDataProvider(solarActivityDataProvider);
        data2.setSolarRadiationPressureEllipsoid(solarRadiationPressureEllipsoid);
        data2.setSunThirdBodyAttraction(sunThirdBodyAttraction);
        data2.setTerrestrialTides(terrestrialTides);
        data2.setVenusThirdBodyAttraction(venusThirdBodyAttraction);
        data2.setCoriolisRelativisticEffect(coriolisRelativisticEffect);
        data2.setLenseThirringRelativisticEffect(lenseThirringRelativisticEffect);
        data2.setSchwarzschildRelativisticEffect(schwarzschildRelativisticEffect);

        // Test the getters when the data are inform by the setters
        Assert.assertEquals(earthPotentialAttractionModel, data2.getEarthPotentialAttractionModel());
        Assert.assertEquals(solarActivityDataProvider, data2.getSolarActivityDataProvider());
        Assert.assertEquals(solarRadiationPressureEllipsoid, data2.getSolarRadiationPressureEllipsoid());
        Assert.assertEquals(rediffusedRadiationPressure, data2.getRediffusedRadiationPressure());
        Assert.assertEquals(moonThirdBodyAttraction, data2.getMoonThirdBodyAttraction());
        Assert.assertEquals(sunThirdBodyAttraction, data2.getSunThirdBodyAttraction());
        Assert.assertEquals(venusThirdBodyAttraction, data2.getVenusThirdBodyAttraction());
        Assert.assertEquals(marsThirdBodyAttraction, data2.getMarsThirdBodyAttraction());
        Assert.assertEquals(jupiterThirdBodyAttraction, data2.getJupiterThirdBodyAttraction());
        Assert.assertEquals(oceanTides, data2.getOceanTides());
        Assert.assertEquals(terrestrialTides, data2.getTerrestrialTides());
        Assert.assertEquals(dragForce, data2.getDragForce());
        Assert.assertEquals(coriolisRelativisticEffect, data2.getCoriolisRelativisticEffect());
        Assert.assertEquals(lenseThirringRelativisticEffect, data2.getLenseThirringRelativisticEffect());
        Assert.assertEquals(schwarzschildRelativisticEffect, data2.getSchwarzschildRelativisticEffect());

        // Compare the propagation with the list of original force datas and
        // with
        // the force datas of the ForceDataModel
        final SpacecraftState spacecraftOriginal = this.propagate(forceModelsListOriginal, assembly);
        final SpacecraftState spacecraftModel = this.propagate(forceModelsList, assembly);
        Assert.assertEquals(
            0,
            spacecraftOriginal.getPVCoordinates().getPosition()
                .subtract(spacecraftModel.getPVCoordinates().getPosition()).getNorm(), 0);

        // Update assembly
        final Assembly assembly2 = assembly;
        data.updateAssembly(assembly2);
        final SpacecraftState spacecraftModel2 = this.propagate(data.getForceModelsList(), assembly2);
        Assert.assertEquals(
            0,
            spacecraftOriginal.getPVCoordinates().getPosition()
                .subtract(spacecraftModel2.getPVCoordinates().getPosition()).getNorm(), 0);
    }

    /**
     * Propagate a spacecraft with a list of force model specific
     * 
     * @param forceList
     *        the list of force model
     * @param assembly
     *        the vehicle assembly
     * @return the spacecraft after propagation
     * @throws PatriusException
     */
    private SpacecraftState propagate(final List<ForceModel> forceList, final Assembly assembly)
                                                                                                throws PatriusException {
        // Initial orbit
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(6871.0e3, 0.001, MathLib.toRadians(30.0),
            MathLib.toRadians(90.0),
            MathLib.toRadians(45.0), MathLib.toRadians(0.0), PositionAngle.MEAN, FramesFactory.getGCRF(),
            new AbsoluteDate(2005, 8, 15, 0, 0, 0.0, TimeScalesFactory.getTAI()),
            3.986004418E14);

        // Initialize the propagator
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(6);
        final NumericalPropagator prop = new NumericalPropagator(integrator);

        prop.setOrbitType(OrbitType.CARTESIAN);
        prop.setSlaveMode();

        // Setup celestial bodies
        final List<CelestialBody> bodies = new ArrayList<CelestialBody>();
        bodies.add(CelestialBodyFactory.getSun());
        bodies.add(CelestialBodyFactory.getMoon());

        // Setup mass model
        final MassModel massProv = new MassModel(assembly);
        // Initial state of the spacecraft
        final AttitudeLaw law = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, law.getAttitude(initialOrbit), massProv);
        prop.setInitialState(initialState);
        prop.setMassProviderEquation(massProv);
        prop.setAttitudeProvider(law);
        // Add the force models to the propagator
        for (final ForceModel force : forceList) {
            prop.addForceModel(force);
        }

        final AbsoluteDate tf = initialOrbit.getDate().shiftedBy(0.1 * 86400.);
        // Propagate
        final SpacecraftState state = prop.propagate(tf);
        return state;
    }

    /**
     * Get sphere model (for vehicle geometry)
     * 
     * @param mass
     *        Satellite mass
     * @return Assembly
     * @throws PatriusException
     */
    public static Assembly getAssembly() throws PatriusException {

        // Create a vehicle
        final Vehicle vehicle = new Vehicle();

        // Add some properties to it
        vehicle.setMainShape(new Sphere(Vector3D.ZERO, 2.));

        // Mass property : dry mass
        final double dryMass = 1000.;
        vehicle.setDryMass(dryMass);

        // Solar panels : 45° from main part
        final double panelArea = 10.;
        vehicle.addSolarPanel(Vector3D.PLUS_I.add(Vector3D.PLUS_K), panelArea);
        vehicle.addSolarPanel(Vector3D.MINUS_I.add(Vector3D.PLUS_K), panelArea);

        // Two engines
        final double thrust1 = 1000.;
        final double isp1 = 320.;
        final double thrust2 = 270.;
        final double isp2 = 150.;
        vehicle.addEngine("Engine1", new PropulsiveProperty(thrust1, isp1));
        vehicle.addEngine("Engine2", new PropulsiveProperty(thrust2, isp2));

        // One tank
        vehicle.addTank("Tank", new TankProperty(500.));

        // Aerodynamic properties
        final double cx = 1.7;
        final double cz = 0.;
        vehicle.setAerodynamicsProperties(cx, cz);

        // Reflectivity property
        final double ka = 1.;
        final double ks = 0.;
        final double kd = 0.;
        final double absorptionCoef = 1.0;
        final double specularCoef = 0.0;
        final double diffuseCoef = 0.0;
        vehicle.setRadiativeProperties(ka, ks, kd, absorptionCoef, specularCoef, diffuseCoef);

        // Create the assembly with default multiplicative factors on mass,
        // drag/SPR area equals to 1.
        return vehicle.createAssembly(FramesFactory.getGCRF());
    }
}
