/**
 * Copyright 2021-2021 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3105:10/05/2022:[PATRIUS] Corrections javadoc 
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.analysis.function;

import fr.cnes.sirius.patrius.math.analysis.differentiation.DerivativeStructure;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * Cosine function of the form c.cos(f(x)) with f an univariate function that returns an angle in radians.
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class CosineFunction implements UnivariateDifferentiableFunction {

     /** Serializable UID. */
    private static final long serialVersionUID = -3228995072031705873L;

    /** Multiplicative coefficient. */
    private final double c;
    
    /** Inner function. */
    private final UnivariateDifferentiableFunction f;

    /**
     * Constructor.
     * Generic formula for cosine function is c.cos(f(x))
     * @param c multiplicative coefficient
     * @param f an univariate function that returns an angle in radians
     */
    public CosineFunction(final double c, final UnivariateDifferentiableFunction f) {
        this.c = c;
        this.f = f;
    }

    /** {@inheritDoc} */
    @Override
    public double value(final double x) {
        return c * MathLib.cos(f.value(x));
    }

    /** {@inheritDoc}
     * <p>Assumes t is has only one variable</p> 
     */
    @Override
    public DerivativeStructure value(final DerivativeStructure t) {
        final double fprime = f.value(t).getPartialDerivative(1);
        final double res = value(t.getValue());
        final double resPrime = -c * fprime * MathLib.sin(f.value(t.getValue()));
        return new DerivativeStructure(1, 1, res, resPrime);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Double.toString(c) + " * cos(" + f.toString() +")"; 
    }
}
