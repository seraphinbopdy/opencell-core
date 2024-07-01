package org.meveo.api.rest.impl;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;

/**
 * A mock implementation of ResponseBuilder
 */
public class ResponseBuilderMock extends ResponseBuilder {
    int status = 0;
    String reasonPhrase = null;
    Object entity = null;
    private Headers<Object> headers;

    @Override
    public Response build() {
        // return new Response() {

        // @Override
        // public int getStatus() {
        // return status;
        // }

        // @Override
        // public StatusType getStatusInfo() {
        // return null;
        // }

        // @Override
        // public Object getEntity() {
        // return entity;
        // }

        // @Override
        // public <T> T readEntity(Class<T> entityType) {
        // return null;
        // }

        // @Override
        // public <T> T readEntity(GenericType<T> entityType) {
        // return null;
        // }

        // @Override
        // public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        // return null;
        // }

        // @Override
        // public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        // return null;
        // }

        // @Override
        // public boolean hasEntity() {
        // return entity != null;
        // }

        // @Override
        // public boolean bufferEntity() {
        // return false;
        // }

        // @Override
        // public void close() {

        // }

        // @Override
        // public MediaType getMediaType() {
        // return MediaType.APPLICATION_JSON;
        // }

        // @Override
        // public Locale getLanguage() {
        // return null;
        // }

        // @Override
        // public int getLength() {
        // return 0;
        // }

        // @Override
        // public Set<String> getAllowedMethods() {
        // return null;
        // }

        // @Override
        // public Map<String, NewCookie> getCookies() {
        // return null;
        // }

        // @Override
        // public EntityTag getEntityTag() {
        // return null;
        // }

        // @Override
        // public Date getDate() {
        // return null;
        // }

        // @Override
        // public Date getLastModified() {
        // return null;
        // }

        // @Override
        // public URI getLocation() {
        // return null;
        // }

        // @Override
        // public Set<Link> getLinks() {
        // return null;
        // }

        // @Override
        // public boolean hasLink(String relation) {
        // return false;
        // }

        // @Override
        // public Link getLink(String relation) {
        // return null;
        // }

        // @Override
        // public Builder getLinkBuilder(String relation) {
        // return null;
        // }

        // @Override
        // public MultivaluedMap<String, Object> getMetadata() {
        // return null;
        // }

        // @Override
        // public MultivaluedMap<String, String> getStringHeaders() {
        // return null;
        // }

        // @Override
        // public String getHeaderString(String name) {
        // return null;
        // }
        // };

        return new ServerResponse(entity, status, headers);

    }

    @Override
    public ResponseBuilder clone() {
        return this;
    }

    @Override
    public ResponseBuilder status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public ResponseBuilder status(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public ResponseBuilder entity(Object entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public ResponseBuilder entity(Object entity, Annotation[] annotations) {
        this.entity = entity;
        return this;
    }

    @Override
    public ResponseBuilder allow(String... methods) {
        return this;
    }

    @Override
    public ResponseBuilder allow(Set<String> methods) {
        return this;
    }

    @Override
    public ResponseBuilder cacheControl(CacheControl cacheControl) {
        return this;
    }

    @Override
    public ResponseBuilder encoding(String encoding) {
        return this;
    }

    @Override
    public ResponseBuilder header(String name, Object value) {
        return this;
    }

    @Override
    public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        return this;
    }

    @Override
    public ResponseBuilder language(String language) {
        return this;
    }

    @Override
    public ResponseBuilder language(Locale language) {
        return this;
    }

    @Override
    public ResponseBuilder type(MediaType type) {
        return this;
    }

    @Override
    public ResponseBuilder type(String type) {
        return this;
    }

    @Override
    public ResponseBuilder variant(Variant variant) {
        return this;
    }

    @Override
    public ResponseBuilder contentLocation(URI location) {
        return this;
    }

    @Override
    public ResponseBuilder cookie(NewCookie... cookies) {
        return this;
    }

    @Override
    public ResponseBuilder expires(Date expires) {
        return this;
    }

    @Override
    public ResponseBuilder lastModified(Date lastModified) {
        return this;
    }

    @Override
    public ResponseBuilder location(URI location) {
        return this;
    }

    @Override
    public ResponseBuilder tag(EntityTag tag) {
        return this;
    }

    @Override
    public ResponseBuilder tag(String tag) {
        return this;
    }

    @Override
    public ResponseBuilder variants(Variant... variants) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'variants'");
    }

    @Override
    public ResponseBuilder variants(List<Variant> variants) {
        return this;
    }

    @Override
    public ResponseBuilder links(Link... links) {
        return this;
    }

    @Override
    public ResponseBuilder link(URI uri, String rel) {
        return this;
    }

    @Override
    public ResponseBuilder link(String uri, String rel) {
        return this;
    }
}