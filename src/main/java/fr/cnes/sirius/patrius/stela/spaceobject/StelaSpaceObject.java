package fr.cnes.sirius.patrius.stela.spaceobject;

import fr.cnes.sirius.patrius.stela.forces.drag.*;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

import java.io.Serializable;

/**
 * This class represents the space object used in STELA.<br>
 * In the future, should be replaced with the {@link fr.cnes.sirius.patrius.stela.StelaSpacecraftFactory}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaSpaceObject implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 216242667787489259L;
    /**
     * Mass of the space object (kg).
     */
    private double mass;
    /**
     * Drag area of the space object (m<sup>2</sup>).
     */
    private double dragArea;
    /**
     * Reflecting area of the space object (m<sup>2</sup>).
     */
    private double reflectingArea;
    /**
     * Reflectivity coefficient of the space object.
     */
    private double reflectivityCoefficient;
    /**
     * Drag coefficient of the space object.
     */
    private AbstractStelaDragCoef dragCoef;
    /**
     * Name of the space object.
     */
    private String name;

    /**
     * Constructor.
     * 
     * @param name
     *        the name
     * @param mass
     *        mass of the space object (kg)
     * @param dragArea
     *        drag surface of the space object (m<sup>2</sup>)
     * @param reflectingArea
     *        reflecting area of the space object (m<sup>2</sup>)
     * @param reflectCoef
     *        reflection coefficient of the space object
     * @param dragCoef
     *        drag coefficient of the space object
     */
    public StelaSpaceObject(final String name, final double mass, final double dragArea, final double reflectingArea,
                            final double reflectCoef, final AbstractStelaDragCoef dragCoef) {
        this.mass = mass;
        this.dragArea = dragArea;
        this.reflectingArea = reflectingArea;
        this.reflectivityCoefficient = reflectCoef;
        this.dragCoef = dragCoef;
        this.name = name.replaceAll("∞", "Infinity");
    }

    /**
     * Basis constructor.
     */
    public StelaSpaceObject() {
        this(Constants.STELA_SPACE_OBJECT_NAME, Constants.STELA_SPACE_OBJECT_MASS,
                Constants.STELA_SPACE_OBJECT_MEAN_AREA, Constants.STELA_SPACE_OBJECT_REF_AREA,
                Constants.STELA_SPACE_OBJECT_REFLECT_COEF, null);
    }

    // ************************** GETTERS ****************************** //

    /**
     * Gets the name of the object.
     *
     * @return the name of the object as a String.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the mass of the object.
     *
     * @return the mass of the object as a double.
     */
    public final double getMass() {
        return this.mass;
    }

    /**
     * Gets the drag coefficient associated with the object.
     *
     * @return the drag coefficient as an instance of AbstractStelaDragCoef.
     */
    public AbstractStelaDragCoef getDragCoef() {
        return this.dragCoef;
    }

    /**
     * Gets the drag area of the object.
     *
     * @return the drag area as a double.
     */
    public final double getDragArea() {
        return this.dragArea;
    }

    /**
     * Gets the reflecting area of the object.
     *
     * @return the reflecting area as a double.
     */
    public final double getReflectingArea() {
        return this.reflectingArea;
    }

    /**
     * Gets the reflection coefficient of the object.
     *
     * @return the reflection coefficient as a double.
     */
    public final double getReflectionCoef() {
        return this.reflectivityCoefficient;
    }

    // ************************** SETTERS ****************************** //

    /**
     * Sets the name of the object.
     * Replaces any occurrence of "∞" with "Infinity".
     *
     * @param name the new name of the object
     */
    public final void setName(final String name) {
        this.name = name.replaceAll("∞", "Infinity");
    }

    /**
     * Sets the mass of the object.
     *
     * @param mass the new mass of the object
     */
    public final void setMass(final double mass) {
        this.mass = mass;
    }

    /**
     * Sets the drag coefficient associated with the object.
     *
     * @param dragCoef the new drag coefficient
     */
    public final void setDragCoef(final AbstractStelaDragCoef dragCoef) {
        this.dragCoef = dragCoef;
    }

    /**
     * Sets the mean drag area of the object.
     *
     * @param dragArea the new drag area
     */
    public final void setMeanArea(final Double dragArea) {
        this.dragArea = dragArea;
    }

    /**
     * Sets the reflecting area of the object.
     *
     * @param reflectingArea the new reflecting area
     */
    public final void setReflectingArea(final Double reflectingArea) {
        this.reflectingArea = reflectingArea;
    }

    /**
     * Sets the reflection coefficient of the object.
     * Logs the new reflection coefficient value.
     *
     * @param reflectivityCoefficient the new reflection coefficient
     */
    public final void setReflectionCoef(final Double reflectivityCoefficient) {
        this.reflectivityCoefficient = reflectivityCoefficient;
    }

    // ************************** OTHER METHODS ****************************** //

    /**
     * Get information on the space object in a string.
     *
     * @param dragSwitch drag switch (true if drag is taken into account)
     * @param srpSwitch SRP switch (true if SRP is taken into account)
     *
     * @return space object information in a string
     */
    public String getInformation(final boolean dragSwitch, final boolean srpSwitch) throws PatriusException {

        // Initialize the results
        final StringBuilder result = new StringBuilder();
        final String CR = System.lineSeparator();

        // Fill the String "res" with the attributes of SpaceObject
        result.append("[ Space Object ]").append(CR);
        // Mass
        result.append(" Mass : ").append(this.mass).append(" kg").append(CR);

        if (dragSwitch) {
            // Drag Area
            result.append(" Drag Area : ").append(this.dragArea).append(" m^2").append(CR);
        }

        if (srpSwitch) {
            // Reflecting Area
            result.append(" Reflecting Area : ").append(this.reflectingArea).append(" m^2").append(CR);
            // Reflection coefficient
            result.append(" Reflectivity Coefficient : ").append(this.reflectivityCoefficient).append(CR);
        }

        // Add to the String only if the drag coefficient is constant
        if (dragSwitch && this.dragCoef.getDragCoefType() == StelaDragCoefType.CONSTANT) {
            // Drag coefficient
            result.append(" Drag Coefficient : ").append(this.dragCoef.getDragCoef(null)).append(CR);
        }

        // Name
        result.append(" Name : ").append(this.name).append(CR);
        return result.toString();
    }

    /**
     * Reset the reflectivity Coefficient of SpaceObject to {@link Constants#STELA_SPACE_OBJECT_REFLECT_COEF}.
     */
    public void reSetReflectivityCoef() {
        this.reflectivityCoefficient = Constants.STELA_SPACE_OBJECT_REFLECT_COEF;
    }

    /**
     * Copy space object.
     * 
     * @return the copied space object
     */
    public StelaSpaceObject copy() throws PatriusException {
        return new StelaSpaceObject(this.name, this.mass, this.dragArea, this.reflectingArea,
            this.reflectivityCoefficient,
            this.dragCoef.copy());
    }
}
