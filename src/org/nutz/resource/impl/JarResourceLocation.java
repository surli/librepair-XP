package org.nutz.resource.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.nutz.lang.Streams;
import org.nutz.resource.NutResource;

public class JarResourceLocation extends ResourceLocation {

    protected List<String> names = new ArrayList<String>();

    protected URI uri;

    public String id() {
        return uri.toString();
    }

    public void scan(String base, Pattern regex, List<NutResource> list) {
        for (final String ensName : names) {
            if (!ensName.startsWith(base))
                continue;
            String name = ensName;
            if (name.contains("/"))
                name = name.substring(name.lastIndexOf('/') + 1);
            if (null == regex || regex.matcher(name).find()) {
                NutResource nutResource = new NutResource() {
                    public InputStream getInputStream() throws IOException {
                        return new URL(uriJarPrefix(uri,"!/" + ensName)).openStream();
                    }
                    
                    public int hashCode() {
                        return (id() + ":" + ensName).hashCode();
                    }
                    
                    public String toString() {
                        return uriJarPrefix(uri, "!/" + ensName);
                    }
                };
                if (ensName.equals(base))
                    nutResource.setName(ensName);
                else
                    nutResource.setName(ensName.substring(base.length()));
                nutResource.setSource(id() + ":" + ensName);
                nutResource.setPriority(75);
                list.add(nutResource);
            }
        }
    }

    public String toString() {
        return id();
    }

    public JarResourceLocation(URL url) throws IOException {
        JarURLConnection jarConn = null;
        JarFile jf = null;
        try {
            this.uri = url.toURI();
            jarConn = (JarURLConnection) new URL(uriJarPrefix(url.toURI(), "!/")).openConnection();
            jf = jarConn.getJarFile();
            Enumeration<JarEntry> ens = jf.entries();
            while (ens.hasMoreElements()) {
                JarEntry en = ens.nextElement();
                names.add(en.getName());
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        finally {
            Streams.safeClose(jf);
        }
    }

    public JarResourceLocation(String jarPath) throws IOException {
        this(new File(jarPath).toURI().toURL());
    }

    private String uriJarPrefix(URI uri, String suffix) {
        String uriString = uri.toString();
        if (uriString.startsWith("jar:")) {
            return uriString + suffix;
        } else {
            return "jar:" + uriString + suffix;
        }
    }
}
