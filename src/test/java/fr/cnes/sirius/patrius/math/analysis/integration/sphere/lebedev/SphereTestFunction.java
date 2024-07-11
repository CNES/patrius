/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1490:26/04/2018: major change to Coppola architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.integration.sphere.lebedev;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;

public class SphereTestFunction implements LebedevFunction {

    private final int number;

    public SphereTestFunction(final int number) {
        this.number = number;
    }

    public double value(final double x, final double y, final double z) {
        final double x2 = MathLib.pow(x, 2);
        final double y2 = MathLib.pow(y, 2);
        final double z2 = MathLib.pow(z, 2);
        final double x4 = MathLib.pow(x, 4);
        final double y5 = MathLib.pow(y, 5);

        double val;

        double ax, ay, az;

        switch (this.number) {
            case 0:
                val = 1.0;
                break;
            case 1:
                val = 1.0 + x + y2 + x2 * y + x4 + y5 + x2 * y2 * z2;
                break;
            case 2:
                val = 0.0;
                ax = -MathLib.pow(9 * x - 2, 2) / 4.0;
                ay = -MathLib.pow(9 * y - 2, 2) / 4.0;
                az = -MathLib.pow(9 * z - 2, 2) / 4.0;
                val += 0.75 * MathLib.exp(ax + ay + az);
                ax = -MathLib.pow(9 * x + 1, 2) / 49.0;
                ay = -MathLib.pow(9 * y + 1, 1) / 10.0;
                az = -MathLib.pow(9 * z + 1, 1) / 10.0;
                val += 0.75 * MathLib.exp(ax + ay + az);
                ax = -MathLib.pow(9 * x - 7, 2) / 4.0;
                ay = -MathLib.pow(9 * y - 3, 2) / 4.0;
                az = -MathLib.pow(9 * z - 5, 2) / 4.0;
                val += 0.50 * MathLib.exp(ax + ay + az);
                ax = -MathLib.pow(9 * x - 4, 2);
                ay = -MathLib.pow(9 * y - 7, 2);
                az = -MathLib.pow(9 * z - 5, 2);
                val += -0.2 * MathLib.exp(ax + ay + az);
                break;
            case 3:
                val = (1.0 + MathLib.tanh(-9 * x - 9 * y + 9 * z)) / 9.0;
                break;
            case 4:
                val = (1.0 + MathLib.signum(-9 * x - 9 * y + 9 * z)) / 9.0;
                break;
            default:
                final String msg = String.format("Unsupported function number (%d)", this.number);
                throw new IllegalStateException(msg);
        }

        return val;
    }

    @Override
    public double value(final LebedevGridPoint point) {
        final double x = point.getX();
        final double y = point.getY();
        final double z = point.getZ();
        return this.value(x, y, z);
    }

    /**
     * Returns the reference value depending of the test function considered.
     * <p>
     * This value corresponds to the integral of the function over the surface of the unit sphere.
     * </p>
     *
     * @return the exact value of the integral of the function over the surface of the unit sphere
     */
    public double getIntegralValue() {
        final double val;

        switch (this.number) {
            case 0:
                val = FastMath.PI * 4.0;
                break;
            case 1:
                val = FastMath.PI * 216.0 / 35.0;
                break;
            case 2:
                val = 6.6961822200736179523;
                break;
            case 3:
                val = FastMath.PI * 4.0 / 9.0;
                break;
            case 4:
                val = FastMath.PI * 4.0 / 9.0;
                break;
            default:
                final String msg = String.format("Unsupported function number (%d)", this.number);
                throw new IllegalStateException(msg);
        }

        return val;
    }
}
