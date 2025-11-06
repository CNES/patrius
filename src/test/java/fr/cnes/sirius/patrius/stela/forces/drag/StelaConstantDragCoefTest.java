package fr.cnes.sirius.patrius.stela.forces.drag;

import static org.junit.Assert.assertEquals;

import fr.cnes.sirius.patrius.math.util.Precision;
import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;

/**
 * Class test for {@link StelaConstantDragCoef}
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-388:25/04/2025:[STELA-PATRIUS] Coefficients de frottement Cook, tabule
 * END-HISTORY
 * @since 4.16
 */
public class StelaConstantDragCoefTest {

    /**
     * Default constant drag coefficient used in tests
     */
    private final StelaConstantDragCoef defaultConstantDragCoef = new StelaConstantDragCoef();

    /**
     * Test method for {@link StelaConstantDragCoef#getDragCoef(StelaDragCoefInput)} and default dragCoef = 2.2
     */
    @Test
    public void testGetDragCoef() {
        final double dragCoef0 = this.defaultConstantDragCoef.getDragCoef(new StelaDragCoefInput());
        final double dragCoef1 = this.defaultConstantDragCoef.getDragCoef(new StelaDragCoefInput(new Vector3D(10000,
            100, 155)));

        assertEquals(2.2, dragCoef0, Precision.DOUBLE_COMPARISON_EPSILON);
        assertEquals(2.2, dragCoef1, Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Test method for {@link StelaConstantDragCoef#StelaConstantDragCoef(double)}
     */
    @Test
    public void testConstructor() {
        final double newDragCoef = 2.72;
        final StelaConstantDragCoef cdc = new StelaConstantDragCoef(newDragCoef);

        assertEquals(newDragCoef, cdc.getDragCoef(new StelaDragCoefInput()), Precision.DOUBLE_COMPARISON_EPSILON);
        assertEquals(newDragCoef, cdc.getDragCoef(new StelaDragCoefInput(0, 0, 0)),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : {@link StelaConstantDragCoef#toString()}
     */
    @Test
    public void testToString() {
        final String CR = System.lineSeparator();
        Assert.assertEquals("Drag Coefficient Type : CONSTANT" + CR + "Constant Drag Coef : 2.2",
            this.defaultConstantDragCoef.toString());
    }

    /**
     * Method tested : {@link StelaConstantDragCoef#getStatInformation()}
     */
    @Test
    public void testGetInformation() {
        Assert.assertEquals("2.2", this.defaultConstantDragCoef.getStatInformation());
    }

    /**
     * Method tested : {@link StelaConstantDragCoef#copy()}
     */
    @Test
    public void testCopy() {
        final StelaConstantDragCoef copy = this.defaultConstantDragCoef.copy();
        Assert.assertNotEquals(this.defaultConstantDragCoef, copy);
        Assert.assertEquals(this.defaultConstantDragCoef.getDragCoef(new StelaDragCoefInput()),
            copy.getDragCoef(new StelaDragCoefInput()),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * Method tested : {@link AbstractStelaDragCoef#getDragCoefType()}
     */
    @Test
    public void testAbstractGetDragCoefType() {
        Assert.assertEquals(StelaDragCoefType.CONSTANT, this.defaultConstantDragCoef.getDragCoefType());
    }

}
