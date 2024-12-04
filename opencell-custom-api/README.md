# Endpoint

An endpoint allows a user to expose a script as a REST endpoint. User can configure the URL path to be exposed as, the method to access it (POST or GET) and specify mappings of request parameters to script properties.

Endpoint can be executed in synchronous or asynchonous mode. In asynchronous mode, an UUID is returned so response can be retrieved later.

## How to define input for a script

When writing a script, any setter (method starting by "set") will be considered as an option for input. To add description to this input, you can simply write a Javadoc for the setter or provide a description in endpoint parameter definition.

Some input parameters, indicating the delay and budget allowed for the execution, are set automatically from request header :

 - maxBudget (Double)  : come from request header **Budget-Max-Value** 
 - budgetUnit (String) : in Joule if not set, come from header **Budget-Unit**
 - maxDelay (Long)  : come from request header **Delay-Max-Value** 
 - delayUnit ([TimeUnit](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/TimeUnit.html)) : in Second if not set, come from header **Delay-Unit**
 
It is the responsibility of the Script to implement that its execution is done within the given budget and delay.

When script is executed in an asynchronous way and the max delay time has passed, script should stop the execution.

The request (and response in synchronous case) are set in the parameters `request` and `response` respectively.

## GUI and API

CRUD for endpoint is available on both administration GUI and API.

The endpoint definition management API is accessible at /api/rest/customization/endpoint. It is implemented in the class org.meveo.api.endpoint.impl.EndpointResourceImpl and accepts a dto of type org.meveo.api.dto.endpoint.EndpointDto for create and update REST operations.

## How to use an Endpoint

See class org.meveo.web.endpoint.EndpointServlet.

The path to access the exposed endpoint depends on the configuration of the endpoint itself. It will always start with "/api/rest/custom/", followed by the basePath of the endpoint (which is its code if not overriden), and the path parameter (which is just the ordered list of path parameters, if not overridem).
An endpoint is accessible via the URL <opencellURL>/api/rest/custom/<endpoint.basePath>/<endpoint.pathParameters>.

The parameters are passed as query parameters for GET type endpoint and as JSON in the body of the request
for POST or PUT type endpoint.

There are several headers that are used to modify the default behavior of the endpoint: 

 - **Keep-Data:** indicates we don't want to remove the execution result from cache when asynchronous results are accessed
 - **Wait-For-Finish:** indicates that we want to wait until one execution finishes and get results after. (Otherwise returns status 102)
 
To retrieve the result of an asynchronous request, call the servlet “/api/rest/custom/” followed by the previously returned ID

Also, for all endpoint execution, the currentUser can be accessed from the endpoint script parameters (key is Script.CONTEXT_CURRENT_USER).
## Examples

For the following examples, let’s consider that we have three setters defined, and therefore three inputs named *companyName*, *year* and *location* and that *companyName* has been used as a path parameter.

### GET Synchronous endpoint

We should first call the creation rest service `POST on /endpoint` with JSON: 

```
{
	"serviceCode": "fr.score3",
	"code": "get-synchronous-endpoint",
	"synchronous": true,
	"method": "GET",
	"pathParameters": [
		{ "companyName"	}
	],
	"parameterMappings": [
		{
			"serviceParameter": "year",
			"parameterName": "creationDate",
			"defaultValue": "2010"
		},
		{
			"serviceParameter": "location",
			"parameterName": "headOffice",
			"defaultValue": "Paris",
			"description": "Office location"
		}
	]
	
}
```

So, the endpoint generated will be accessible with GET method under /api/rest/custom/get-synchronous-endpoint/Opencell?creationDate=2011&headOffice=Dijon and the result will be returned once the script has been executed.

### POST Asynchronous endpoint

For asynchronous rest service lets specify attribute *synchronous=false*:

```
{
	"serviceCode": "fr.score3",
	"code": "asynchronous-endpoint",
	"synchronous": false,
	"method": "POST",
	"pathParameters": [
		{"companyName"}
	],
	"parameterMappings": [
		{
			"serviceParameter": "year",
			"parameterName": "creationDate",
			"defaultValue": "2010"
		},
		{
			"serviceParameter": "location",
			"parameterName": "headOffice",
			"defaultValue": "Paris",
			"description": "Office location"
		}
	]
	
}
```

So, the endpoint generated will be accessible with POST method under /api/rest/custom/asynchronous-endpoint/Opencell

The request of the body should be:

```
{
	"creationDate": 2011,
	"headOffice": "Dijon"
}
```

The execution will return an UUID like “6bbb2e71-8361-4d51-887d-91c2a52d08f0” and the script will be executed. So, to retrieve the result, we need to call the endpoint /api/rest/custom/6bbb2e71-8361-4d51-887d-91c2a52d08f0. If the execution is still not finished when we make the request, it will return with code 102. If we want to wait until finish and get result, we must set the header “Wait-For-Finish” to true.

## How to access the POST or PUT body in Script

The POST body parameters are passed to script via setter methods and are also added into the context map parameter of the Script's execute method. In our example above, if we call the POST API with the following JSON body:

```
{
	"creationDate": 2011,
	"headOffice": "Dijon"
}
```

Then we can access both JSON fields in the script as:

```
this.getCreationDate();
this.getHeadOffice() 

And/Or

context.get("creationDate");
context.get("headOffice");
```

if you need the original body you can retrieve it from the `REQUEST_BODY` parameter.

	
	
## JSONata

In the field  jsonataTransformer  you can provide a JSONata expression to transform the output of the endpoint.

For a response that returns a map of values a JSONata expression *"$.amountWithoutTax + $.amountTax"* would return 121 as a final response value

```
{
	"amountWithoutTax": 100,
	"amountTax": 21
}
```
	
## OpenAPI definition
	
Go to ```GET /api/rest/customization/endpoint/openApi/{code}```

## Access management

To execute a secure endpoint (secured = true), a user needs to have the corresponding role. The name of this role follows the pattern `Custom_API_{endpointCode}-access`.
It is generated whenever we create a secured endpoint.

When we update an endpoint from secure to unsecure, the role is deleted. Inversely, from unsecure to secure, the role is created.

Once the role is created, it is added to the role `Custom_API-AccessAll`. This role also belongs by default to the `administrateur` role.


## Pooling

When an endpoint is heavily solicited, it might be advantageous to use an instance pool for its associated script instances.

The configuration elements for script are :

- `usePool` : Default to `false`. When true, enable the usage of a pool for the related endpoint.
- `min`: The minimum number of instances in the pool. When set > 0, some script will be instantiated at startup to fill the pool.
- `max`: The maximum number of instances in the pool. When set, if the maximum instances are reach, the pool will the block endpoint's request until one instance become available. 
- `maxIdleTime`: The max idle time, in seconds, before eviction. When set, if an instance is idling for more than the defined amout of time, it will be destroyed.

### Script example

```java
package org.meveo.script;

import java.util.UUID;
import java.util.Map;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class PooledScriptExample extends Script {

    private static Logger log = LoggerFactory.getLogger(PooledScriptExample.class);

    private String uuid;

    private String input;

    public void setInput(String input) {
        this.input = input;
    }

    // This function is called when object is created by the pool.
    // The parameters object will always be null.
    @Override
    public void init(Map<String, Object> parameters) throws BusinessException {
        uuid = UUID.randomUUID().toString();
        log.info("Script with uuid {} created !", uuid);
    }

  	// This function is called when the object is borrowed by the pool, before the inputs are initialized by the endpoint.
    @Override
    public void resetState() {
        this.input = "default";
        log.info("Activating script with uuid {}", uuid);
    }

    // This function is called when object is executed by the endpoint.
    // The parameters object will contain the usual parameters and the fields bound to setter will be initialized.
    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        log.info("Script with uuid {} called with input {}", uuid, input);
    }

    // This function is called when object is destroyed by the pool.
    // This is where you would close objects that need to be closed.
    // The parameters object will always be null
    @Override
    public void finalize(Map<String, Object> parameters) throws BusinessException {
        log.info("Script with uuid {} will be destroyed", uuid);
    }
}
```

### Script example

```json
{
    "code": "org.meveo.script.ScriptEndpoint",
    "script": "package org.meveo.script; ......",
    "pool": {
        "usePool": true,
        "min": "2",
        "max": "5",
        "maxIdleTime": "10"
    }
}
```
