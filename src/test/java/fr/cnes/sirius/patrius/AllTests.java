package fr.cnes.sirius.patrius;

/**
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history
 * version 1.0 - Sylvain VRESK - code creation
 */

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import fr.cnes.sirius.patrius.utils.AngleIntervalTest;
import fr.cnes.sirius.patrius.utils.AngleToolsTest;
import fr.cnes.sirius.patrius.utils.ComparatorsTest;
import fr.cnes.sirius.patrius.utils.GenericIntervalTest;
import fr.cnes.sirius.patrius.utils.PatriusMessagesTest;

/**
 * @description Sirius tests suite.
 * 
 * @author Sylvain VRESK
 * 
 * @version $Id$
 * 
 * @since 1.0
 * 
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({ PatriusMessagesTest.class, ComparatorsTest.class, GenericIntervalTest.class,
    AngleIntervalTest.class, AngleToolsTest.class })
public class AllTests {

}
