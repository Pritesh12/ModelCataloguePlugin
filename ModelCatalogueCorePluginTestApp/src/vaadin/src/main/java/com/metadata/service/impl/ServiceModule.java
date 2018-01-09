package com.metadata.service.impl;

import com.google.inject.AbstractModule;

import com.metadata.service.api.MatchService;

//is being loaded by guice automatically
@SuppressWarnings("unused")
class ServiceModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(MatchService.class).to(MatchServiceImpl.class);
    }
}
