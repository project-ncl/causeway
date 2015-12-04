package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.error.BindException;
import org.junit.BeforeClass;

/**
 * Created by jdcasey on 12/3/15.
 */
public class AbstractKojiModelTest
{
    protected static KojiBindery bindery;

    @BeforeClass
    public static void setup()
            throws Exception
    {
        bindery = new KojiBindery();
    }
}
