package org.nutz.ioc.loader.annotation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nutz.dao.impl.NutDao;
import org.nutz.ioc.IocLoader;
import org.nutz.ioc.ObjectLoadException;
import org.nutz.ioc.ObjectProxy;
import org.nutz.ioc.impl.NutIoc;
import org.nutz.ioc.loader.annotation.meta.Issue1060;
import org.nutz.ioc.meta.IocObject;
import org.nutz.json.Json;
import org.nutz.log.Logs;

public class AnnotationIocLoaderTest {

    IocLoader iocLoader = new AnnotationIocLoader("org.nutz.ioc.loader.annotation.meta");

    @Test
    public void testGetName() {
        assertNotNull(iocLoader.getName());
        assertTrue(iocLoader.getName().length > 0);
    }

    @Test
    public void testHas() {
        assertTrue(iocLoader.has("classA"));
    }

    @Test
    public void testLoad() throws Throwable {
        IocObject iocObject = iocLoader.load(null, "classB");
        assertNotNull(iocObject);
        assertNotNull(iocObject.getFields());
        assertTrue(iocObject.getFields().size() == 1);
        System.out.println(Json.toJson(iocObject));
        assertEquals("refer", iocObject.getFields().values().iterator().next().getValue().getType());
    }

    @Test
    public void test_ioc_inject_by_setter() throws ObjectLoadException {
        AnnotationIocLoader loader = new AnnotationIocLoader(getClass().getPackage().getName());
        Logs.get().error(loader.load(null, "issue1060"));
        NutIoc ioc = new NutIoc(loader);
        ioc.getIocContext().save("app", "dao", new ObjectProxy(new NutDao())); // 放个假的
        ioc.get(Issue1060.class);
        ioc.depose();
    }
}
