/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web.templ;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.groovy.GroovyTemplateEngine;
import io.vertx.ext.web.templ.groovy.impl.GroovyTemplateEngineImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@RunWith(VertxUnitRunner.class)
public class GroovyTemplateTest {

  private static Vertx vertx;

  @BeforeClass
  public static void before() {
    vertx = Vertx.vertx(new VertxOptions().setFileSystemOptions(new FileSystemOptions().setFileCachingEnabled(true)));
  }

  @Test
  public void testTemplateHandlerOnClasspath(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);

    final JsonObject context = new JsonObject()
      .put("foo", "badger")
      .put("bar", "fox");

    context.put("context", new JsonObject().put("path", "/test-mvel-template2.templ"));

    String tmplPath = "somedir/test-mvel-template2.templ".replace('/', File.separatorChar);
    engine.render(context, tmplPath).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("Hello badger and fox\nRequest path is /test-mvel-template2.templ\n", normalizeCRLF(render.toString()));
    }));
  }

  @Test
  public void MVELTemplateTestMVELTemplateTestMVELTemplateTest(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);

    final JsonObject context = new JsonObject()
      .put("foo", "badger")
      .put("bar", "fox");

    context.put("context", new JsonObject().put("path", "/test-mvel-template3.templ"));

    String tmplPath = "src/test/filesystemtemplates/test-mvel-template3.templ".replace('/', File.separatorChar);
    engine.render(context, tmplPath).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("Hello badger and fox\nRequest path is /test-mvel-template3.templ\n", normalizeCRLF(render.toString()));
    }));
  }

  @Test
  public void testTemplateHandlerWithInclude(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);

    final JsonObject context = new JsonObject()
      .put("foo", "badger")
      .put("bar", "fox");

    context.put("context", new JsonObject().put("path", "/test-mvel-template4.templ"));

    String tmplPath = "src/test/filesystemtemplates/test-mvel-template4.templ".replace('/', File.separatorChar);
    engine.render(context, tmplPath).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("Hello badger and fox\nRequest path is /test-mvel-template4.templ", normalizeCRLF(render.toString()));
    }));
  }

  @Test
  public void testTemplateHandlerOnClasspathDisableCaching(TestContext should) {
    System.setProperty("vertxweb.environment", "development");
    testTemplateHandlerOnClasspath(should);
  }

  @Test
  public void testTemplateHandlerNoExtension(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);

    final JsonObject context = new JsonObject()
      .put("foo", "badger")
      .put("bar", "fox");

    context.put("context", new JsonObject().put("path", "/test-mvel-template2.templ"));

    String tmplPath = "somedir/test-mvel-template2".replace('/', File.separatorChar);
    engine.render(context, tmplPath).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("Hello badger and fox\nRequest path is /test-mvel-template2.templ\n", normalizeCRLF(render.toString()));
    }));
  }

  @Test
  public void testTemplateHandlerChangeExtension(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx, "bempl");

    final JsonObject context = new JsonObject()
      .put("foo", "badger")
      .put("bar", "fox");

    context.put("context", new JsonObject().put("path", "/test-mvel-template2"));

    String tmplPath = "somedir/test-mvel-template2".replace('/', File.separatorChar);
    engine.render(context, tmplPath).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("Cheerio badger and fox\nRequest path is /test-mvel-template2\n", normalizeCRLF(render.toString()));
    }));
  }

  @Test
  public void testNoSuchTemplate(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);
    engine.render(new JsonObject(), "nosuchtemplate.templ").onComplete(should.asyncAssertFailure());
  }

  @Test
  public void testCachingEnabled(TestContext should) throws IOException {
    System.setProperty("vertxweb.environment", "production");
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);

    PrintWriter out;
    File temp = File.createTempFile("template", ".templ", new File("target/classes"));
    temp.deleteOnExit();

    out = new PrintWriter(temp);
    out.print("before");
    out.flush();
    out.close();

    engine.render(new JsonObject(), temp.getParent() + File.separatorChar + temp.getName()).onComplete(should.asyncAssertSuccess(render -> {
      should.assertEquals("before", render.toString());
      // cache is enabled so if we change the content that should not affect the result

      try {
        PrintWriter out2 = new PrintWriter(temp);
        out2.print("after");
        out2.flush();
        out2.close();
      } catch (IOException e) {
        should.fail(e);
      }

      engine.render(new JsonObject(), temp.getParent() + File.separatorChar + temp.getName()).onComplete(should.asyncAssertSuccess(render2 -> {
        should.assertEquals("before", render2.toString());
      }));

    }));
  }

  // For windows testing
  private static String normalizeCRLF(String s) {
    return s.replace("\r\n", "\n");
  }

  @Test
  public void trimEol() {
    assertEquals("\r foo ", GroovyTemplateEngineImpl.trimRightEol("\r foo \n"));
    assertEquals("\r foo ", GroovyTemplateEngineImpl.trimRightEol("\r foo \r"));
    assertEquals("\r foo ", GroovyTemplateEngineImpl.trimRightEol("\r foo \r\n"));
    assertEquals("\r foo ", GroovyTemplateEngineImpl.trimRightEol("\r foo \n\r"));
    assertEquals("\r foo \r", GroovyTemplateEngineImpl.trimRightEol("\r foo \r\r\n"));
  }

  @Test
  public void testTemplateFailure(TestContext should) {
    TemplateEngine engine = GroovyTemplateEngine.create(vertx);
    engine.render(new JsonObject(), "somedir/bad.templ").onComplete(should.asyncAssertFailure(e->{
      should.assertEquals("java.lang.ArithmeticException: Division by zero", e.toString());
    }));
  }

}
