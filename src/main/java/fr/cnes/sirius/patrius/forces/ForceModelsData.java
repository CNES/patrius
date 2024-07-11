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
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:767:24/04/2018: Creation of ForceModelsData to collect the models data for a force computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.tides.OceanTides;
import fr.cnes.sirius.patrius.forces.gravity.tides.TerrestrialTides;
import fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationPressure;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureEllipsoid;
import fr.cnes.sirius.patrius.forces.relativistic.CoriolisRelativisticEffect;
import fr.cnes.sirius.patrius.forces.relativistic.LenseThirringRelativisticEffect;
import fr.cnes.sirius.patrius.forces.relativistic.SchwarzschildRelativisticEffect;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Class gathering force models in one single class. This class be used then to configure a
 * propagator and modify/retrieve forces one by one.
 * 
 * @author Marie Capot
 * 
 * @since 4.1
 * 
 * @version $Id: MSISE2000.java 17794 2017-09-01 07:15:56Z bignon $
 */
public class ForceModelsData {

    /** Earth potential attraction model (Balmino, Droziner, Cunningham...) */
    private ForceModel earthPotentialAttractionModel;

    /** Solar activity data provider */
    private SolarActivityDataProvider solarActivityDataProvider;

    /** Solar radiation pressure ellipsoid */
    private SolarRadiationPressureEllipsoid solarRadiationPressureEllipsoid;

    /** Rediffused radiation pressure */
    private RediffusedRadiationPressure rediffusedRadiationPressure;

    /** Moon perturbation force model. */
    private ThirdBodyAttraction moonThirdBodyAttraction;

    /** Sun perturbation force model. */
    private ThirdBodyAttraction sunThirdBodyAttraction;

    /** Venus perturbation force model. */
    private ThirdBodyAttraction venusThirdBodyAttraction;

    /** Mars perturbation force model. */
    private ThirdBodyAttraction marsThirdBodyAttraction;

    /** Jupiter perturbation force model. */
    private ThirdBodyAttraction jupiterThirdBodyAttraction;

    /** Ocean tides */
    private OceanTides oceanTides;

    /** Terrestrial tides */
    private TerrestrialTides terrestrialTides;

    /** Drag force */
    private DragForce dragForce;

    /** Coriolis relativistic effect */
    private CoriolisRelativisticEffect coriolisRelativisticEffect;

    /** Lense thirring relativistic effect */
    private LenseThirringRelativisticEffect lenseThirringRelativisticEffect;

    /** Schwarzschild relativistic effect */
    private SchwarzschildRelativisticEffect schwarzschildRelativisticEffect;

    /**
     * Default constructor.
     */
    public ForceModelsData() {
        // Nothing to do
    }

    /**
     * Constructor.
     * 
     * @param earthPotentialAttractionModelIn the attraction model of earth potential
     * @param solarActivityDataProviderIn solar activity data provider
     * @param solarRadiationPressureEllipsoidIn model of the solar radiation pressure
     * @param rediffusedRadiationPressureIn model of rediffused radiation pressure
     * @param moonThirdBodyAttractionIn force model of the third body attraction: the moon
     * @param sunThirdBodyAttractionIn force model of the third body attraction: the sun
     * @param venusThirdBodyAttractionIn force model of the third body attraction: venus
     * @param marsThirdBodyAttractionIn force model of the third body attraction: mars
     * @param jupiterThirdBodyAttractionIn force model of the third body attraction: jupiter
     * @param oceanTidesIn ocean tides
     * @param terrestrialTidesIn terrestrial tides
     * @param dragForceIn drag force
     * @param coriolisRelativisticEffectIn Coriolis relativistic effect
     * @param lenseThirringRelativisticEffectIn Lense-Thirring relativistic effect
     * @param schwarzschildRelativisticEffectIn Schwarzschild relativistic effect
     */
    public ForceModelsData(final ForceModel earthPotentialAttractionModelIn,
        final SolarActivityDataProvider solarActivityDataProviderIn,
        final SolarRadiationPressureEllipsoid solarRadiationPressureEllipsoidIn,
        final RediffusedRadiationPressure rediffusedRadiationPressureIn,
        final ThirdBodyAttraction moonThirdBodyAttractionIn,
        final ThirdBodyAttraction sunThirdBodyAttractionIn,
        final ThirdBodyAttraction venusThirdBodyAttractionIn,
        final ThirdBodyAttraction marsThirdBodyAttractionIn,
        final ThirdBodyAttraction jupiterThirdBodyAttractionIn, final OceanTides oceanTidesIn,
        final TerrestrialTides terrestrialTidesIn, final DragForce dragForceIn,
        final CoriolisRelativisticEffect coriolisRelativisticEffectIn,
        final LenseThirringRelativisticEffect lenseThirringRelativisticEffectIn,
        final SchwarzschildRelativisticEffect schwarzschildRelativisticEffectIn) {
        // Forces
        this.earthPotentialAttractionModel = earthPotentialAttractionModelIn;
        this.dragForce = dragForceIn;
        this.solarRadiationPressureEllipsoid = solarRadiationPressureEllipsoidIn;
        this.rediffusedRadiationPressure = rediffusedRadiationPressureIn;
        this.moonThirdBodyAttraction = moonThirdBodyAttractionIn;
        this.sunThirdBodyAttraction = sunThirdBodyAttractionIn;
        this.venusThirdBodyAttraction = venusThirdBodyAttractionIn;
        this.marsThirdBodyAttraction = marsThirdBodyAttractionIn;
        this.jupiterThirdBodyAttraction = jupiterThirdBodyAttractionIn;
        this.terrestrialTides = terrestrialTidesIn;
        this.oceanTides = oceanTidesIn;
        this.schwarzschildRelativisticEffect = schwarzschildRelativisticEffectIn;
        this.coriolisRelativisticEffect = coriolisRelativisticEffectIn;
        this.lenseThirringRelativisticEffect = lenseThirringRelativisticEffectIn;

        // Data
        this.solarActivityDataProvider = solarActivityDataProviderIn;
    }

    /**
     * Method to update the force models depending on the assembly (DragForce,
     * CustomSolarRadiationPressureEllipsoidCircular and CustomRediffusedRadiationPressure).
     * 
     * @param assembly to update the force models depending on it.
     * @throws PatriusException if the assembly does not have only one valid aerodynamic property
     *         or if it has no radiative properties found
     */
    public void updateAssembly(final Assembly assembly) throws PatriusException {
        if (this.dragForce != null) {
            this.dragForce = new DragForce(this.dragForce, assembly);
        }
        if (this.solarRadiationPressureEllipsoid != null) {
            this.solarRadiationPressureEllipsoid = new SolarRadiationPressureEllipsoid(
                this.solarRadiationPressureEllipsoid, assembly);
        }
        if (this.rediffusedRadiationPressure != null) {

            this.rediffusedRadiationPressure = new RediffusedRadiationPressure(
                this.rediffusedRadiationPressure, assembly);

        }
    }

    /**
     * @return the Earth potential model
     */
    public ForceModel getEarthPotentialAttractionModel() {
        return this.earthPotentialAttractionModel;
    }

    /**
     * @param earthPotentialAttractionModelIn the Earth potential model to set
     */
    public void setEarthPotentialAttractionModel(
                                                 final ForceModel earthPotentialAttractionModelIn) {
        this.earthPotentialAttractionModel = earthPotentialAttractionModelIn;
    }

    /**
     * @return the solar activity data provider
     */
    public SolarActivityDataProvider getSolarActivityDataProvider() {
        return this.solarActivityDataProvider;
    }

    /**
     * @param solarActivityDataProviderIn the solar activity data provider to set
     */
    public void setSolarActivityDataProvider(
                                             final SolarActivityDataProvider solarActivityDataProviderIn) {
        this.solarActivityDataProvider = solarActivityDataProviderIn;
    }

    /**
     * @return the solar radiation pressure
     */
    public SolarRadiationPressureEllipsoid getSolarRadiationPressureEllipsoid() {
        return this.solarRadiationPressureEllipsoid;
    }

    /**
     * @param srp the solar radiation pressure to set
     */
    public void
            setSolarRadiationPressureEllipsoid(final SolarRadiationPressureEllipsoid srp) {
        this.solarRadiationPressureEllipsoid = srp;
    }

    /**
     * @return the rediffused radiation pressure
     */
    public RediffusedRadiationPressure getRediffusedRadiationPressure() {
        return this.rediffusedRadiationPressure;
    }

    /**
     * @param rediffusedRadiationPressureIn the rediffused radiation pressure to set
     */
    public void setRediffusedRadiationPressure(
                                               final RediffusedRadiationPressure rediffusedRadiationPressureIn) {
        this.rediffusedRadiationPressure = rediffusedRadiationPressureIn;
    }

    /**
     * @return the Moon attraction
     */
    public ThirdBodyAttraction getMoonThirdBodyAttraction() {
        return this.moonThirdBodyAttraction;
    }

    /**
     * @param moonThirdBodyAttractionIn the Moon attraction to set
     */
    public void setMoonThirdBodyAttraction(final ThirdBodyAttraction moonThirdBodyAttractionIn) {
        this.moonThirdBodyAttraction = moonThirdBodyAttractionIn;
    }

    /**
     * @return the Sun attraction
     */
    public ThirdBodyAttraction getSunThirdBodyAttraction() {
        return this.sunThirdBodyAttraction;
    }

    /**
     * @param sunThirdBodyAttractionIn the Sun attraction to set
     */
    public void setSunThirdBodyAttraction(final ThirdBodyAttraction sunThirdBodyAttractionIn) {
        this.sunThirdBodyAttraction = sunThirdBodyAttractionIn;
    }

    /**
     * @return the Venus attraction
     */
    public ThirdBodyAttraction getVenusThirdBodyAttraction() {
        return this.venusThirdBodyAttraction;
    }

    /**
     * @param venusThirdBodyAttractionIn the Venus attraction to set
     */
    public void setVenusThirdBodyAttraction(final ThirdBodyAttraction venusThirdBodyAttractionIn) {
        this.venusThirdBodyAttraction = venusThirdBodyAttractionIn;
    }

    /**
     * @return the Mars attraction
     */
    public ThirdBodyAttraction getMarsThirdBodyAttraction() {
        return this.marsThirdBodyAttraction;
    }

    /**
     * @param marsThirdBodyAttractionIn the Mars attraction to set
     */
    public void setMarsThirdBodyAttraction(final ThirdBodyAttraction marsThirdBodyAttractionIn) {
        this.marsThirdBodyAttraction = marsThirdBodyAttractionIn;
    }

    /**
     * @return the Jupiter attraction
     */
    public ThirdBodyAttraction getJupiterThirdBodyAttraction() {
        return this.jupiterThirdBodyAttraction;
    }

    /**
     * @param jupiterThirdBodyAttractionIn the Jupiter attraction to set
     */
    public void setJupiterThirdBodyAttraction(
                                              final ThirdBodyAttraction jupiterThirdBodyAttractionIn) {
        this.jupiterThirdBodyAttraction = jupiterThirdBodyAttractionIn;
    }

    /**
     * @return the oceanic tides
     */
    public OceanTides getOceanTides() {
        return this.oceanTides;
    }

    /**
     * @param oceanTidesIn the oceanic tides to set
     */
    public void setOceanTides(final OceanTides oceanTidesIn) {
        this.oceanTides = oceanTidesIn;
    }

    /**
     * @return the terrestrial tides
     */
    public TerrestrialTides getTerrestrialTides() {
        return this.terrestrialTides;
    }

    /**
     * @param terrestrialTidesIn the terrestrial tides to set
     */
    public void setTerrestrialTides(final TerrestrialTides terrestrialTidesIn) {
        this.terrestrialTides = terrestrialTidesIn;
    }

    /**
     * @return the drag force
     */
    public DragForce getDragForce() {
        return this.dragForce;
    }

    /**
     * @param dragForceIn the drag force to set
     */
    public void setDragForce(final DragForce dragForceIn) {
        this.dragForce = dragForceIn;
    }

    /**
     * @return the Coriolis relativistic effect
     */
    public CoriolisRelativisticEffect getCoriolisRelativisticEffect() {
        return this.coriolisRelativisticEffect;
    }

    /**
     * @param coriolisRelativisticEffectIn the Coriolis relativistic effect to set
     */
    public void setCoriolisRelativisticEffect(
                                              final CoriolisRelativisticEffect coriolisRelativisticEffectIn) {
        this.coriolisRelativisticEffect = coriolisRelativisticEffectIn;
    }

    /**
     * @return the Lense Thirring relativistic effect
     */
    public LenseThirringRelativisticEffect getLenseThirringRelativisticEffect() {
        return this.lenseThirringRelativisticEffect;
    }

    /**
     * @param lenseThirring the Lense Thirring relativistic effect to set
     */
    public void setLenseThirringRelativisticEffect(final LenseThirringRelativisticEffect lenseThirring) {
        this.lenseThirringRelativisticEffect = lenseThirring;
    }

    /**
     * @return the Schwarzschild relativistic effect
     */
    public SchwarzschildRelativisticEffect getSchwarzschildRelativisticEffect() {
        return this.schwarzschildRelativisticEffect;
    }

    /**
     * @param schwarzschildRelativistic the Schwarzschild relativistic effect to set
     */
    public void setSchwarzschildRelativisticEffect(final SchwarzschildRelativisticEffect schwarzschildRelativistic) {
        this.schwarzschildRelativisticEffect = schwarzschildRelativistic;
    }

    /**
     * Returns a list of all added force models.
     * 
     * @return the force models as a list
     */
    public List<ForceModel> getForceModelsList() {
        // Initialization
        final List<ForceModel> forceModelList = new ArrayList<ForceModel>();
        // Add the force model data to the force model list if the model data exists
        this.addModel(this.earthPotentialAttractionModel, forceModelList);
        this.addModel(this.solarRadiationPressureEllipsoid, forceModelList);
        this.addModel(this.rediffusedRadiationPressure, forceModelList);
        this.addModel(this.moonThirdBodyAttraction, forceModelList);
        this.addModel(this.sunThirdBodyAttraction, forceModelList);
        this.addModel(this.venusThirdBodyAttraction, forceModelList);
        this.addModel(this.marsThirdBodyAttraction, forceModelList);
        this.addModel(this.jupiterThirdBodyAttraction, forceModelList);
        this.addModel(this.oceanTides, forceModelList);
        this.addModel(this.terrestrialTides, forceModelList);
        this.addModel(this.dragForce, forceModelList);
        this.addModel(this.coriolisRelativisticEffect, forceModelList);
        this.addModel(this.lenseThirringRelativisticEffect, forceModelList);
        this.addModel(this.schwarzschildRelativisticEffect, forceModelList);
        // Return the list of force models
        // to be used in propagation
        return forceModelList;
    }

    /**
     * Add force to list
     * 
     * @param force force
     * @param list list of forces
     */
    private void addModel(final ForceModel force, final List<ForceModel> list) {
        if (force != null) {
            list.add(force);
        }
    }
}
