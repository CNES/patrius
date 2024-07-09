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
 * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION::DM:1950:14/11/2018:new attitude profile design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * Represents an attitude profile.
 *
 * @author morerem
 *
 * @since 4.2
 */
public interface AttitudeProfile extends AttitudeLeg {

    /** {@inheritDoc} */
    @Override
    AttitudeProfile copy(final AbsoluteDateInterval newInterval);
}
