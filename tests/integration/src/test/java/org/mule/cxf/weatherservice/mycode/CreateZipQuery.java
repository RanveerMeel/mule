/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.cxf.weatherservice.mycode;

import org.mule.cxf.weatherservice.myweather.GetCityWeatherByZIP;


public class CreateZipQuery
{

	/**
	 * Create a request to query by zip code.
	 *
	 * @param input
	 * @return
	 */
	public GetCityWeatherByZIP createRequest(Object input) {
		GetCityWeatherByZIP request = new GetCityWeatherByZIP();
		request.setZIP("30075");
		return request;
	}
}