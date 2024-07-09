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
 * @history 29/04/2015 (creation)
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This handles univariate functions. It is the space mechanics counter part of {@link UnivariateFunction}.
 **
 * @author Emmanuel Bignon
 * 
 * @version $Id: UnivariateDateFunction.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.0
 * 
 */
public interface UnivariateDateFunction extends Serializable {

    /**
     * Returns value of function at provided date.
     * 
     * @param date
     *        a date
     * @return value of function at provided date
     */
    double value(final AbsoluteDate date);
}
