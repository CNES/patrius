package fr.cnes.sirius.patrius.stela.spaceobject;

/** HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 */
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaConstantDragCoef;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaVariableDragCoef;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import org.junit.Assert;
import org.junit.Test;

public class StelaSpaceObjectTest {

    /**
     * Test the default constructor <br>
     *
     * Methods tested :<br>
     * - {@link StelaSpaceObject#StelaSpaceObject()}<br>
     * - {@link StelaSpaceObject#getName()}<br>
     * - {@link StelaSpaceObject#getMass()} <br>
     * - {@link StelaSpaceObject#getDragArea()} <br>
     * - {@link StelaSpaceObject#getReflectingArea()} <br>
     * - {@link StelaSpaceObject#getReflectionCoef()} <br>
     */
    @Test
    public void testDefaultConstructor() {
        final StelaSpaceObject spaceObjectActual = new StelaSpaceObject();

        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_NAME, spaceObjectActual.getName());
        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_MASS, spaceObjectActual.getMass(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_MEAN_AREA, spaceObjectActual.getDragArea(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_REF_AREA, spaceObjectActual.getReflectingArea(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_REFLECT_COEF, spaceObjectActual.getReflectionCoef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test all setters <br>
     *
     * Methods tested :<br>
     * - {@link StelaSpaceObject#StelaSpaceObject(String, double, double, double, double, StelaAbstractDragCoef)} <br>
     * - {@link StelaSpaceObject#setName(String)} <br>
     * - {@link StelaSpaceObject#setMass(double)} <br>
     * - {@link StelaSpaceObject#setDragCoef(StelaAbstractDragCoef)} <br>
     * - {@link StelaSpaceObject#setReflectingArea(Double)} <br>
     * - {@link StelaSpaceObject#setReflectionCoef(Double)} <br>
     * - {@link StelaSpaceObject#setDragCoef(StelaAbstractDragCoef)} <br>
     * - {@link StelaSpaceObject#reSetReflectivityCoef()}
     */
    @Test
    public void testSetters() throws PatriusException {

        /* Use Constructor */

        final String name1 = "Test";
        final double mass1 = 0.1;
        final double dragArea1 = 0.2;
        final double reflecArea1 = 0.3;
        final double reflecCoef1 = 0.4;
        final double dragCoef1 = 0.5;

        final StelaSpaceObject spaceObjectActual =
            new StelaSpaceObject(name1, mass1, dragArea1, reflecArea1, reflecCoef1,
                new StelaConstantDragCoef(dragCoef1));

        Assert.assertEquals(name1, spaceObjectActual.getName());
        Assert.assertEquals(mass1, spaceObjectActual.getMass(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragArea1, spaceObjectActual.getDragArea(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(reflecArea1, spaceObjectActual.getReflectingArea(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(reflecCoef1, spaceObjectActual.getReflectionCoef(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragCoef1, spaceObjectActual.getDragCoef().getDragCoef(null),
            Precision.DOUBLE_COMPARISON_EPSILON);

        /* Use setters */

        final String name2 = "Test 2";
        final double mass2 = 1.1;
        final double dragArea2 = 1.2;
        final double reflecArea2 = 1.3;
        final double reflecCoef2 = 1.4;
        final double dragCoef2 = 1.5;

        spaceObjectActual.setName(name2);
        spaceObjectActual.setMass(mass2);
        spaceObjectActual.setMeanArea(dragArea2);
        spaceObjectActual.setReflectingArea(reflecArea2);
        spaceObjectActual.setReflectionCoef(reflecCoef2);
        spaceObjectActual.setDragCoef(new StelaConstantDragCoef(dragCoef2));

        Assert.assertEquals(name2, spaceObjectActual.getName());
        Assert.assertEquals(mass2, spaceObjectActual.getMass(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragArea2, spaceObjectActual.getDragArea(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(reflecArea2, spaceObjectActual.getReflectingArea(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(reflecCoef2, spaceObjectActual.getReflectionCoef(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dragCoef2, spaceObjectActual.getDragCoef().getDragCoef(null),
            Precision.DOUBLE_COMPARISON_EPSILON);

        /* Use reSetReflectivityCoef */

        spaceObjectActual.reSetReflectivityCoef();
        Assert.assertEquals(Constants.STELA_SPACE_OBJECT_REFLECT_COEF, spaceObjectActual.getReflectionCoef(),
                Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested :<br>
     * - {@link StelaSpaceObject#getInformation(boolean, boolean)}
     */
    @Test
    public void testGetInformation() throws PatriusException {
        final StelaSpaceObject spaceObjectActual = new StelaSpaceObject();
        final double dragCoef = 2.5;
        spaceObjectActual.setDragCoef(new StelaConstantDragCoef(dragCoef));

        final String CR = System.lineSeparator();
        Assert.assertEquals(
            "[ Space Object ]" + CR + " Mass : " + Constants.STELA_SPACE_OBJECT_MASS + " kg" + CR
                    + " Drag Area : " + Constants.STELA_SPACE_OBJECT_MEAN_AREA + " m^2" + CR
                    + " Reflecting Area : " + Constants.STELA_SPACE_OBJECT_REF_AREA + " m^2" + CR
                    + " Reflectivity Coefficient : " + Constants.STELA_SPACE_OBJECT_REFLECT_COEF + CR
                    + " Drag Coefficient : " + dragCoef + CR
                    + " Name : " + Constants.STELA_SPACE_OBJECT_NAME + CR,
            spaceObjectActual.getInformation(true, true));

        Assert.assertEquals("[ Space Object ]" + CR
                + " Mass : " + Constants.STELA_SPACE_OBJECT_MASS + " kg" + CR
                + " Reflecting Area : " + Constants.STELA_SPACE_OBJECT_REF_AREA + " m^2" + CR
                + " Reflectivity Coefficient : " + Constants.STELA_SPACE_OBJECT_REFLECT_COEF + CR
                + " Name : " + Constants.STELA_SPACE_OBJECT_NAME + CR,
            spaceObjectActual.getInformation(false, true));

        // Set variable drag coef
        spaceObjectActual.setDragCoef(new StelaVariableDragCoef(null, null));

        Assert.assertEquals(
            "[ Space Object ]" + CR + " Mass : " + Constants.STELA_SPACE_OBJECT_MASS + " kg" + CR
                    + " Drag Area : " + Constants.STELA_SPACE_OBJECT_MEAN_AREA + " m^2" + CR
                    + " Reflecting Area : " + Constants.STELA_SPACE_OBJECT_REF_AREA + " m^2" + CR
                    + " Reflectivity Coefficient : " + Constants.STELA_SPACE_OBJECT_REFLECT_COEF + CR
                    + " Name : " + Constants.STELA_SPACE_OBJECT_NAME + CR,
            spaceObjectActual.getInformation(true, true));

        Assert.assertEquals(
            "[ Space Object ]" + CR + " Mass : " + Constants.STELA_SPACE_OBJECT_MASS + " kg" + CR
                    + " Name : " + Constants.STELA_SPACE_OBJECT_NAME + CR,
            spaceObjectActual.getInformation(false, false));
    }

    /**
     * Method tested :<br>
     * - {@link StelaSpaceObject#copy()}
     */
    @Test
    public void copy() throws PatriusException {
        final StelaSpaceObject spaceObjectExpected =
            new StelaSpaceObject("Test", 0.1, 0.2, 0.3, 0.4,
                new StelaConstantDragCoef(0.5));
        final StelaSpaceObject spaceObjectActual = spaceObjectExpected.copy();

        Assert.assertNotEquals(spaceObjectExpected, spaceObjectActual);
        Assert.assertEquals(spaceObjectExpected.getName(), spaceObjectActual.getName());
        Assert.assertEquals(spaceObjectExpected.getMass(), spaceObjectActual.getMass(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(spaceObjectExpected.getDragArea(), spaceObjectActual.getDragArea(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(spaceObjectExpected.getReflectingArea(), spaceObjectActual.getReflectingArea(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(spaceObjectExpected.getReflectionCoef(), spaceObjectActual.getReflectionCoef(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(spaceObjectExpected.getDragCoef().getDragCoef(null),
            spaceObjectActual.getDragCoef().getDragCoef(null), Precision.DOUBLE_COMPARISON_EPSILON);
    }
}