/**
 * Copyright 2011-2022 CNES
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
 * VERSION:4.15:OPENFD-385:21/11/2024:Execution en parallele des tests concernant EclipticJ2000Provider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.data;

import java.io.File;
import fr.cnes.sirius.patrius.Utils;
import java.io.InputStream;
import fr.cnes.sirius.patrius.Utils;
import java.net.URISyntaxException;
import fr.cnes.sirius.patrius.Utils;
import java.net.URL;
import fr.cnes.sirius.patrius.Utils;
import java.util.regex.Pattern;
import fr.cnes.sirius.patrius.Utils;

import org.junit.Assert;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Before;
import fr.cnes.sirius.patrius.Utils;
import org.junit.Test;
import fr.cnes.sirius.patrius.Utils;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.Utils;

public class ZipJarCrawlerTest {

    @Test
    public void testMultiZipClasspath() throws PatriusException {
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler("zipped-data/multizip.zip").feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    @Test
    public void testMultiZip() throws PatriusException, URISyntaxException {
        final URL url =
            ZipJarCrawlerTest.class.getClassLoader().getResource("zipped-data/multizip.zip");
        final CountingLoader crawler = new CountingLoader();
        new ZipJarCrawler(new File(url.toURI().getPath())).feed(Pattern.compile(".*\\.txt$"), crawler);
        Assert.assertEquals(6, crawler.getCount());
    }

    private static class CountingLoader implements DataLoader {
        private int count = 0;

        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name) {
            ++this.count;
        }

        public int getCount() {
            return this.count;
        }
    }


    @Before
    public void setUp() {
        Utils.clear();
    }
}
