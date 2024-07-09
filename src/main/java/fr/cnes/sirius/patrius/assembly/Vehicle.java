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
 * @history creation 04/04/2017
 *
 * HISTORY
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
* VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1796:07/09/2018:Correction vehicle class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.assembly.models.aerocoeffs.AerodynamicCoefficient;
import fr.cnes.sirius.patrius.assembly.properties.AeroCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroGlobalProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeIRProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.assembly.vehicle.AerodynamicProperties;
import fr.cnes.sirius.patrius.assembly.vehicle.RadiativeProperties;
import fr.cnes.sirius.patrius.assembly.vehicle.VehicleSurfaceModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.CrossSectionProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Vehicle class: it represent a classic satellite: main body + solar panels.
 * This satellite can have tanks, engines and masses.
 * To build a more complex satellite (with sensors, etc.), use {@link Assembly} class.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.0
 */
public class Vehicle implements Serializable {

    /** Main shape. */
    public static final String MAIN_SHAPE = "Main shape";

    /** Serializable UID. */
    private static final long serialVersionUID = -6692571768602555532L;

    /** Main shape frame. */
    private static final String FRAME = "Frame";

    /** Solar panel. */
    private static final String SOLAR_PANEL = "Solar panel";

    /** Engine string. */
    private static final String S_ENGINE_STRING = "Engine";

    /** Tank string. */
    private static final String S_TANK_STRING = "Fuel Tank ";

    /** Vehicle main shape. */
    private CrossSectionProvider mainShape;

    /** Solar panels. */
    private List<Facet> solarPanels;

    /** Engines. */
    private final Map<String, PropulsiveProperty> engines;

    /** Tanks. */
    private final Map<String, TankProperty> tanks;

    /** Vehicle dry mass property. */
    private MassProperty dryMassProperty;

    /** Aerodynamic properties. */
    private AerodynamicProperties aerodynamicProperties;

    /** Radiative properties. */
    private RadiativeProperties radiativeProperties;

    /** Engine count. */
    private int engineCount = 1;

    /** Tank count. */
    private int tankCount = 1;

    /**
     * Constructor. As this class is developed as a builder, vehicle properties are to be set by
     * available setters.
     */
    public Vehicle() {
        // Initialize arrays
        this.solarPanels = new ArrayList<Facet>();
        this.engines = new LinkedHashMap<String, PropulsiveProperty>();
        this.tanks = new LinkedHashMap<String, TankProperty>();
    }

    /**
     * Creates a new instance.
     * 
     * @param mainShapeIn main shape of vehicle
     * @param solarPanelsIn solar panels of vehicle
     * @param dryMassPropertyIn mass property for dry mass
     * @param aerodynamicPropertiesIn aerodynamic properties; it may be null if no aerodynamic
     *        properties are required
     * @param radiativePropertiesIn radiative properties; it may be null if no radiative properties
     *        are required
     * @param enginesList list of engines (propulsive properties); it may be null if no engines list
     *        is required
     * @param fuelTankList list of tanks; it may be null if no fuel tanks are required
     */
    public Vehicle(final CrossSectionProvider mainShapeIn, final List<Facet> solarPanelsIn,
        final MassProperty dryMassPropertyIn,
        final AerodynamicProperties aerodynamicPropertiesIn,
        final RadiativeProperties radiativePropertiesIn,
        final List<PropulsiveProperty> enginesList, final List<TankProperty> fuelTankList) {
        // Set main parts of vehicle
        this.mainShape = mainShapeIn;
        if (solarPanelsIn == null) {
            this.solarPanels = new ArrayList<Facet>();
        } else {
            this.solarPanels = solarPanelsIn;
        }

        // Initialization
        this.engines = new LinkedHashMap<String, PropulsiveProperty>();
        this.tanks = new LinkedHashMap<String, TankProperty>();

        // Get input variables
        // Mass property
        if (dryMassPropertyIn != null) {
            this.dryMassProperty = dryMassPropertyIn;
        }
        // Add aerodynamic properties if necessary
        if (aerodynamicPropertiesIn != null) {
            this.aerodynamicProperties = aerodynamicPropertiesIn;
        }
        // Add radiative properties if necessary
        if (radiativePropertiesIn != null) {
            this.radiativeProperties = radiativePropertiesIn;
        }
        // Add engines if necessary
        if (enginesList != null) {
            for (final PropulsiveProperty engine : enginesList) {
                if (engine.getPartName().isEmpty()) {
                    engine.setPartName(S_ENGINE_STRING + Integer.toString(this.engineCount));
                    this.increaseEngineCounter();
                }
                this.engines.put(engine.getPartName(), engine);
            }
        }
        // Add fuel tanks if necessary
        if (fuelTankList != null) {
            for (final TankProperty tank : fuelTankList) {
                if (tank.getPartName().isEmpty()) {
                    tank.setPartName(S_TANK_STRING + Integer.toString(this.tankCount));
                    this.increaseTankCounter();
                }
                this.tanks.put(tank.getPartName(), tank);
            }
        }
    }

    /**
     * Create an {@link Assembly}. Call to { {@link #createAssembly(Frame, double, double, double)} with default
     * multiplicative factors set to 1.
     *
     * @param frame frame link between tree of frames and spacecraft frame
     * @return an assembly
     * @throws PatriusException thrown if mass is negative
     */
    public Assembly createAssembly(final Frame frame) throws PatriusException {
        return this.createAssembly(frame, 1.0, 1.0, 1.0);
    }

    /**
     * Create an {@link Assembly} with multiplicative coefficients to take into account the change
     * in surface for drag or SRP during a propagation of the change of dry mass. Therefore,
     * multiplicative factor for the mass, the drag/SRP area are to be given as input.
     * <p>
     * Warning: using this method, new tanks are created using cMass coefficients with current tanks. As a result,
     * user-added tanks mass will not vary if using some mass equations.
     * </p>
     *
     * @param frame frame link between tree of frames and spacecraft frame
     * @param cMass multiplicative factor on masses (dry and tanks)
     * @param cDrag multiplicative factor on drag area
     * @param cSRP multiplicative factor on SRP area
     * @return an assembly
     * @throws PatriusException thrown if mass is negative
     */
    public Assembly createAssembly(final Frame frame, final double cMass, final double cDrag,
                                   final double cSRP) throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();

        // Check the main shape is defined
        this.mainShapeCheck();

        // Take into account multiplicative factor cDrag and cSRP
        final CrossSectionProvider extendedMainShapeSRP = this.getTrueMainShapeSRP(cSRP);
        final CrossSectionProvider extendedMainShapeDrag = this.getTrueMainShapeDrag(cDrag);

        // Build main shape of the vehicle and add solar panels if there are any
        builder.addMainPart(MAIN_SHAPE);

        // Build solar panels
        this.buildSolarPanels(builder, cDrag, cSRP);

        // Build tanks and engines
        this.buildTankAndEngines(builder);

        final double dispersedMass = this.getDispersedDryMass(cMass);
        // Add a dry mass property
        builder.addProperty(new MassProperty(dispersedMass), MAIN_SHAPE);

        // Add aero cross section and aero cross section global properties if
        // available
        if (this.aerodynamicProperties != null) {
            if (this.aerodynamicProperties.getLiftCoef() != null && this.aerodynamicProperties.getDragCoef() != null) {
                builder.addProperty(new AeroGlobalProperty(this.aerodynamicProperties.getDragCoef(),
                    this.aerodynamicProperties.getLiftCoef(),
                    extendedMainShapeDrag), MAIN_SHAPE);
            }

            final IPartProperty aeroProperty;
            if (this.mainShape instanceof Sphere) {
                // Use sphere property to avoid computing cross section of a
                // sphere
                aeroProperty = new AeroSphereProperty(((Sphere) this.mainShape).getRadius()
                    * MathLib.sqrt(cDrag), this.aerodynamicProperties.getDragCoef());
            } else {
                aeroProperty =
                    new AeroCrossSectionProperty(extendedMainShapeDrag, this.aerodynamicProperties.getDragCoef());
            }
            builder.addProperty(aeroProperty, MAIN_SHAPE);
        }

        // Add radiative properties if there are any
        if (this.radiativeProperties != null) {
            final IPartProperty radiativeProperty;
            if (this.mainShape instanceof Sphere) {
                // Use sphere property to avoid computing cross section of a
                // sphere
                radiativeProperty = new RadiativeSphereProperty(((Sphere) this.mainShape).getRadius()
                    * MathLib.sqrt(cSRP));
            } else {
                radiativeProperty = new RadiativeCrossSectionProperty(extendedMainShapeSRP);
            }
            builder.addProperty(radiativeProperty, MAIN_SHAPE);
            final double kAbs = this.radiativeProperties.getRadiativeProperty().getAbsorptionRatio().getValue();
            final double kAbsIr = this.radiativeProperties.getRadiativeIRProperty().getAbsorptionCoef().getValue();

            if (!Double.isNaN(kAbs)) {
                final double kSpec =
                    this.radiativeProperties.getRadiativeProperty().getSpecularReflectionRatio().getValue();
                final double kDiff =
                    this.radiativeProperties.getRadiativeProperty().getDiffuseReflectionRatio().getValue();
                builder.addProperty(new RadiativeProperty(kAbs, kSpec, kDiff), MAIN_SHAPE);
            }

            if (!Double.isNaN(kAbsIr)) {
                final double kSpecIr =
                    this.radiativeProperties.getRadiativeIRProperty().getSpecularReflectionCoef().getValue();
                final double kDiffIr =
                    this.radiativeProperties.getRadiativeIRProperty().getDiffuseReflectionCoef().getValue();
                builder.addProperty(new RadiativeIRProperty(kAbsIr, kSpecIr, kDiffIr), MAIN_SHAPE);
            }
        }

        // Link to tree of frames
        builder.initMainPartFrame(new UpdatableFrame(frame, Transform.IDENTITY, FRAME));

        // Return Assembly
        return builder.returnAssembly();
    }

    /**
     * Build tanks and engines (put in another method for checkstyle issues).
     *
     * @param builder assembly builder
     * @throws PatriusException thrown if mass is negative
     */
    private void buildTankAndEngines(final AssemblyBuilder builder) throws PatriusException {
        // Add engines if there are any
        for (final Entry<String, PropulsiveProperty> entry : this.engines.entrySet()) {
            final PropulsiveProperty engine = entry.getValue();
            final String partName = entry.getKey();
            builder.addPart(partName, MAIN_SHAPE, Transform.IDENTITY);
            builder.addProperty(engine, partName);
        }

        // Add tanks if there are any
        for (final Entry<String, TankProperty> entry : this.tanks.entrySet()) {
            // The tank is duplicate because the link isn't enough for Doors
            final TankProperty tank = new TankProperty(entry.getValue().getMass());
            final String partName = entry.getKey();
            builder.addPart(partName, MAIN_SHAPE, Transform.IDENTITY);
            builder.addProperty(tank, partName);

        }
    }

    /**
     * Build solar panels (put in another method for checkstyle issues).
     *
     * @param builder assembly builder
     * @param cDrag multiplicative factor on drag area
     * @param cSRP multiplicative factor on SRP area
     */
    private void buildSolarPanels(final AssemblyBuilder builder, final double cDrag,
                                  final double cSRP) {

        // Add solar panels and aero/ radiative facet properties if panels are
        // defined
        for (int i = 0; i < this.solarPanels.size(); i++) {
            final Facet currentFacet = this.solarPanels.get(i);
            final String partName = SOLAR_PANEL + i;
            builder.addPart(partName, MAIN_SHAPE, Transform.IDENTITY);

            // Aero facet property
            // Define it with drag coef if possible
            if (this.aerodynamicProperties != null) {
                final Facet facet = new Facet(currentFacet.getNormal(), currentFacet.getArea()
                    * cDrag);
                // Property with drag only. Ct not used for this property
                builder.addProperty(
                    new AeroFacetProperty(facet, this.aerodynamicProperties.getDragCoef(), new ConstantFunction(0)),
                    SOLAR_PANEL + i);
            }

            // Radiative facet property
            if (this.radiativeProperties != null) {
                final Facet facet = new Facet(currentFacet.getNormal(), currentFacet.getArea()
                    * cSRP);
                builder.addProperty(new RadiativeFacetProperty(facet), partName);
                final double kAbs = this.radiativeProperties.getRadiativeProperty().getAbsorptionRatio().getValue();
                final double kAbsIr = this.radiativeProperties.getRadiativeIRProperty().getAbsorptionCoef().getValue();
                // Radiative property and IR if available
                if (!Double.isNaN(kAbs)) {
                    final double kSpec =
                        this.radiativeProperties.getRadiativeProperty().getSpecularReflectionRatio().getValue();
                    final double kDiff =
                        this.radiativeProperties.getRadiativeProperty().getDiffuseReflectionRatio().getValue();
                    builder.addProperty(new RadiativeProperty(kAbs, kSpec, kDiff), partName);
                }
                if (!Double.isNaN(kAbsIr)) {
                    final double kSpecIr =
                        this.radiativeProperties.getRadiativeIRProperty().getSpecularReflectionCoef().getValue();
                    final double kDiffIr =
                        this.radiativeProperties.getRadiativeIRProperty().getDiffuseReflectionCoef().getValue();
                    builder.addProperty(new RadiativeIRProperty(kAbsIr, kSpecIr, kDiffIr), partName);
                }
            }
        }
    }

    /**
     * Returns main shape with area multiplied by cDrag.
     *
     * @param cDrag drag multiplicative factor
     * @return main shape with area multiplied by cDrag
     */
    private CrossSectionProvider getTrueMainShapeDrag(final double cDrag) {
        return new CrossSectionProvider(){
            /** {@inheritDoc} */
            @Override
            public double getCrossSection(final Vector3D direction) {
                return Vehicle.this.mainShape.getCrossSection(direction) * cDrag;
            }
        };
    }

    /**
     * Returns main shape with area multiplied by cSRP.
     *
     * @param cSRP drag multiplicative factor
     * @return main shape with area multiplied by cSRP
     */
    private CrossSectionProvider getTrueMainShapeSRP(final double cSRP) {
        return new CrossSectionProvider(){
            /** {@inheritDoc} */
            @Override
            public double getCrossSection(final Vector3D direction) {
                return Vehicle.this.mainShape.getCrossSection(direction) * cSRP;
            }
        };
    }

    /**
     * Get modified dry mass.
     *
     * @param totalMassMultiplicativeFactor total mass multiplicative factor
     * @return the modified dry mass
     */
    private double getDispersedDryMass(final double totalMassMultiplicativeFactor) {
        // New total mass
        final double newTotalMass = this.getTotalMass() * totalMassMultiplicativeFactor;
        // New dry mass
        return newTotalMass - this.getErgolsMass();
    }

    /**
     * Increase the engines counter.
     */
    private void increaseEngineCounter() {
        this.engineCount++;
    }

    /**
     * Increase the tanks counter.
     */
    private void increaseTankCounter() {
        this.tankCount++;
    }

    /**
     * Set the main vehicle shape.
     *
     * @param shape main shape
     */
    public void setMainShape(final CrossSectionProvider shape) {
        this.mainShape = shape;
    }

    /**
     * Add a solar panel to the vehicle.
     *
     * @param normalPanel vector normal to the panel in satellite frame
     * @param areaPanel panel area
     */
    public void addSolarPanel(final Vector3D normalPanel, final double areaPanel) {
        this.solarPanels.add(new Facet(normalPanel, areaPanel));
    }

    /**
     * Add an engine to the vehicle.
     *
     * @param name engine name
     * @param engine an engine
     */
    public void addEngine(final String name, final PropulsiveProperty engine) {
        engine.setPartName(name);
        this.engines.put(name, engine);
    }

    /**
     * Add a tank to the vehicle.
     *
     * @param name tank name
     * @param tank a tank
     */
    public void addTank(final String name, final TankProperty tank) {
        tank.setPartName(name);
        this.tanks.put(name, tank);
    }

    /**
     * Set vehicle dry mass.
     *
     * @param mass dry mass
     * @throws PatriusException problem to the mass property construction
     */
    public void setDryMass(final double mass) throws PatriusException {
        this.dryMassProperty = new MassProperty(mass);
    }

    /**
     * Set aerodynamics properties only if possible : main shape must be a sphere or there must be
     * no solar panels. An exception is thrown otherwise.
     *
     * @param cx drag coefficient function
     * @param cz lift coefficient function
     * @throws PatriusException problem to the aerodynamics properties construction
     * @throws IllegalArgumentException if vehicle is inconsistent for aero properties
     */
    public void setAerodynamicsProperties(final AerodynamicCoefficient cx,
                                          final AerodynamicCoefficient cz) throws PatriusException {
        // Check for vehicle consistency
        this.mainShapeCheck();

        if ((this.mainShape instanceof Sphere) && (this.solarPanels.isEmpty())) {
            this.aerodynamicProperties = new AerodynamicProperties(((Sphere) this.mainShape), cx, cz);

        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_AERO_PROP);
        }

    }

    /**
     * Set aerodynamics properties as constants.
     *
     * @param cx constant drag coefficient
     * @param cz constant lift coefficient
     * @throws PatriusException error to the aerodynamic properties creation
     */
    public void setAerodynamicsProperties(final double cx, final double cz) throws PatriusException {
        this.aerodynamicProperties = new AerodynamicProperties(new VehicleSurfaceModel(this.mainShape), cx,
            cz);

    }

    /**
     * Set radiative properties.
     *
     * @param ka absorption coefficient (visible)
     * @param ks specular coefficient (visible)
     * @param kd diffusion coefficient (visible)
     * @param kaIr absorption coefficient (IR)
     * @param ksIr specular coefficient (IR)
     * @param kdIr diffusion coefficient (IR)
     * @throws PatriusException error to the radiative properties creation
     */
    public void setRadiativeProperties(final double ka, final double ks, final double kd, final double kaIr,
            final double ksIr, final double kdIr) throws PatriusException {
        this.radiativeProperties = new RadiativeProperties(new RadiativeProperty(ka, ks, kd),
            new RadiativeIRProperty(kaIr, ksIr, kdIr), new VehicleSurfaceModel(
                this.mainShape));

    }

    /**
     * Returns the main shape.
     *
     * @return main shape
     */
    public CrossSectionProvider getMainShape() {
        return this.mainShape;
    }

    /**
     * Returns the solar panels list.
     *
     * @return solar panels list
     */
    public List<Facet> getSolarPanelsList() {
        return this.solarPanels;
    }

    /**
     * Returns the engines list.
     *
     * @return engines list
     */
    public List<PropulsiveProperty> getEnginesList() {
        return new ArrayList<PropulsiveProperty>(this.engines.values());
    }

    /**
     * Returns the tanks list.
     *
     * @return tanks list
     */
    public List<TankProperty> getTanksList() {
        return new ArrayList<TankProperty>(this.tanks.values());
    }

    /**
     * Returns dry mass.
     *
     * @return dry mass
     */
    public double getDryMass() {
        return this.dryMassProperty.getMass();
    }

    /**
     * Returns the sum of ergols masses.
     *
     * @return total ergol mass
     */
    public double getErgolsMass() {
        double ergolsMass = 0.0;
        for (final TankProperty tank : this.tanks.values()) {
            ergolsMass += tank.getMass();
        }
        return ergolsMass;
    }

    /**
     * Returns total mass : sum of dry mass and ergol mass.
     *
     * @return total mass
     */
    public double getTotalMass() {
        double dryMass = 0;
        if (this.dryMassProperty != null) {
            dryMass = this.dryMassProperty.getMass();
        }
        return dryMass + this.getErgolsMass();
    }

    /**
     * Returns the aero properties : drag and lift coefficients.
     *
     * @return aero properties
     */
    public IParamDiffFunction[] getAerodynamicsPropertiesFunction() {
        return new IParamDiffFunction[] { this.aerodynamicProperties.getDragCoef(),
            this.aerodynamicProperties.getLiftCoef() };
    }

    /**
     * Returns the radiative properties : absorption, specular and diffusion coefficient (visible
     * and IR), as an array : [0] : absorption coefficient (visible) [1] : specular coefficient
     * (visible) [2] : diffusion coefficient (visible) [3] : absorption coefficient (IR) [4] :
     * specular coefficient (IR) [5] : diffusion coefficient (IR)
     *
     * @return radiative properties
     */
    public double[] getRadiativePropertiesTab() {
        final RadiativeProperty radProp = this.radiativeProperties.getRadiativeProperty();
        final RadiativeIRProperty radIrProp = this.radiativeProperties.getRadiativeIRProperty();
        return new double[] { radProp.getAbsorptionRatio().getValue(), radProp.getSpecularReflectionRatio().getValue(),
            radProp.getDiffuseReflectionRatio().getValue(), radIrProp.getAbsorptionCoef().getValue(),
            radIrProp.getSpecularReflectionCoef().getValue(), radIrProp.getDiffuseReflectionCoef().getValue() };
    }

    /**
     * Public method to search the engine object corresponding to the specified name.
     *
     * @param name name of the engine
     * @return engine object
     */
    public PropulsiveProperty getEngine(final String name) {

        PropulsiveProperty engine = null;
        if (this.engines != null) {
            engine = this.engines.get(name);
        }
        // If there is no list of engines or if engine has not been found return
        // null
        return engine;

    }

    /**
     * Public method to search the tank object corresponding to the specified name.
     *
     * @param name name of the tank
     * @return tank object
     */
    public TankProperty getTank(final String name) {
        TankProperty tank = null;

        if (this.tanks != null) {
            tank = this.tanks.get(name);
        }
        // If there is no list of tanks or if tank has not been found return
        // null
        return tank;

    }

    /**
     * Get mass property.
     *
     * @return the mass property (null if it was not given)
     */
    public MassProperty getMassProperty() {
        return this.dryMassProperty;
    }

    /**
     * Get main shape aerodynamic properties.
     * 
     * @return the main shape aerodynamic properties (null if they were not given)
     */
    public AerodynamicProperties getAerodynamicProperties() {
        return this.aerodynamicProperties;
    }

    /**
     * Get main shape radiative properties.
     * 
     * @return the radiative properties (null if they where not given)
     */
    public RadiativeProperties getRadiativeProperties() {
        return this.radiativeProperties;
    }

    /**
     * Checks if the main shape exists.
     */
    private void mainShapeCheck() {
        if (this.mainShape == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_MAIN_PART);
        }
    }

    /**
     * @return the s_engineCount
     */
    public int getEngineCount() {
        return this.engineCount;
    }

    /**
     * @return the s_tankCount
     */
    public int getTankCount() {
        return this.tankCount;
    }
}
