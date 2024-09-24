/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.math.fitting;

import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealVector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class enables to perform linear regression.
 *
 * @author parraudp @ CS
 */
public final class LinearRegression {

    /** The origin A of the linear model y = A + B*x */
    private final double origin;

    /** The slope B of the linear model y = A + B*x */
    private final double slope;

    /**
     * Constructor
     *
     * @param x
     *        the array of the abscissas
     * @param y
     *        the array of the ordinates
     * @throws PatriusException
     */
    public LinearRegression(final double[] x, final double[] y) {

        // Check the input data
        if (x.length != y.length) {
            throw new PatriusRuntimeException(PatriusMessages.DIMENSION_MISMATCH_REGRESSION, null);
        }
        // Construction of the QR system
        final RealMatrix matrix = MatrixUtils.createRealMatrix(x.length, 2);
        for (int i = 0; i < x.length; i++) {
            matrix.setEntry(i, 0, 1.);
            matrix.setEntry(i, 1, x[i]);
        }

        // Retrieve the solver
        final QRDecomposition qr = new QRDecomposition(matrix);
        final DecompositionSolver solver = qr.getSolver();

        // Solve in least squares sense
        final RealVector yVect = MatrixUtils.createRealVector(y);
        final RealVector model = solver.solve(yVect);

        // Get the coefficients of the linear regression
        this.origin = model.getEntry(0);
        this.slope = model.getEntry(1);
    }

    /**
     * Get the origin A of the linear model y = A + B*x
     *
     * @return the origin of the linear model
     */
    public double getOrigin() {
        return this.origin;
    }

    /**
     * Get the slope B of the linear model y = A + B*x
     *
     * @return the slope of the linear model
     */
    public double getSlope() {
        return this.slope;
    }
}
