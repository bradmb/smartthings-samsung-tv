/**
 *  Samsung Smart TV (Connect)
 *
 *  Author: Brad Butner
 *  Date: 2014-04-20
 *
 *  This code is incomplete. Currently it allows you to find TVs on your network, pick one, and run a quick
 *  test against it. The full device controls have not yet been built.
 *
 *  PLEASE NOTE: This has only been tested on a Samsung UN50EH5300 TV. It may or may not work for you. Also,
 *  you will need to know your device's IP and MAC Address. This information should be viewable from your
 *  TV under Menu > Network > Network Status.
 */
preferences {
	page(name:"televisionDiscovery", title:"Samsung TV Setup", content:"televisionDiscovery", refreshTimeout:5)
	page(name:"televisionAuthenticate", title:"Samsung TV Authentication", content:"televisionAuthenticate", refreshTimeout:5)
	page(name:"televisionTest", title:"Samsung TV Test", content:"televisionTest", refreshTimeout:5)
}

def televisionDiscovery() {
    int tvRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
    state.bridgeRefreshCount = tvRefreshCount + 1
    def refreshInterval = 3

    def options = televisionsDiscovered() ?: []
    def numFound = options.size() ?: 0

    if(!state.subscribe) {
        subscribe(location, null, deviceLocationHandler, [filterEvents:false])    
        state.subscribe = true
    }

    // Television discovery request every 15 seconds
    if((tvRefreshCount % 5) == 0) {
        findTv()
    }

    return dynamicPage(name:"televisionDiscovery", title:"Samsung TV Search Started!", nextPage:"televisionAuthenticate", refreshInterval:refreshInterval, uninstall: true) {
        section("Please wait while we discover your Samsung TV. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
            input "selectedTv", "enum", required:false, title:"Select Samsung TV (${numFound} found)", multiple:false, options:options
        }
    }
}

def televisionAuthenticate() {
    tvAction("AUTHENTICATE")

    return dynamicPage(name:"televisionDiscovery", title:"Samsung TV Search Started!", nextPage:"televisionTest") {
        section("We sent an authentication request to your TV. Please accept the request and click next.") {
        }
    }
}

def televisionTest() {
	tvAction("INFO")

    return dynamicPage(name:"televisionDiscovery", title:"Samsung TV Search Started!", nextPage:"", install:true) {
        section("Your TV has been instructed to show the INFO display. If this appeared, you're all setup!") {
        }
    }
}

Map televisionsDiscovered() {
	def vbridges = getSamsungTvs()
	def map = [:]
	vbridges.each {
    	log.debug "Discovered List: $it"
        def value = "$it"
        def key = it.value
        
        if (key.contains("!")) {
            def settingsInfo = key.split("!")
            def deviceIp = convertHexToIP(settingsInfo[1])
            value = "Samsung TV (${deviceIp})"
        }
        
        map["${key}"] = value
	}
	map
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// Remove UPNP Subscription
	unsubscribe()
	state.subscribe = false

    log.debug "Application Initialized"
    log.debug "Selected TV: $selectedTv"
    
    /* For when we can get actual device types working with the "sendHubCommand"
    addChildDevice("bradbutner", "Samsung TV", [settingIpAddress, settingMacAddress].join('!'))
    log.debug "${getAllChildDevices()}"
    */
}

// Returns a list of the found Samsung TVs from UPNP discovery
def getSamsungTvs()
{
	state.televisions = state.televisions ?: [:]
}

// Sends out a UPNP request, looking for the Samsung TV. Results are sent to [deviceLocationHandler]
private findTv() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:samsung.com:service:MultiScreenService:1", physicalgraph.device.Protocol.LAN))
}

// Parses results from [findTv], looking for the specific UPNP result that clearly identifies the TV we can use
def deviceLocationHandler(evt) {
	//log.debug "Device Location Event: $evt"
	def upnpResult = parseEventMessage(evt.description)
    
    if (upnpResult?.ssdpTerm?.contains("urn:samsung.com:service:MultiScreenService:1")) {
        //log.debug "Found TV: ${upnpResult}"
        state.televisions << [device:"${upnpResult.mac}!${upnpResult.ip}"]
    }
}

// Executes actions against the selected Television
private tvAction(key) {
	log.debug "TV Action Executing: ${key}"
    
    if (selectedTv == null || selectedTv.getBytes().size() == 0) {
    	log.debug "No TV Selected -- Cannot Execute"
        return
    }

    // Standard Connection Data
    def appString = "iphone..iapp.samsung"
    def appStringLength = appString.getBytes().size()

    def tvAppString = "iphone.UN60ES8000.iapp.samsung"
    def tvAppStringLength = tvAppString.getBytes().size()

    def remoteName = "SmartThings".encodeAsBase64().toString()
    def remoteNameLength = remoteName.getBytes().size()

    // Device Connection Data
    def deviceSettings = selectedTv.split("!")
    def ipAddress = convertHexToIP(deviceSettings[1]).encodeAsBase64().toString()
    def ipAddressLength = ipAddress.getBytes().size()
    def ipAddressHex = deviceSettings[1] // settingIpAddress.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join()

    def macAddress = deviceSettings[0] //settingMacAddress.replaceAll(":","").encodeAsBase64().toString()
    def macAddressLength = macAddress.getBytes().size()

	// The Authentication Message
    def authenticationMessage = "${(char)0x64}${(char)0x00}${(char)ipAddressLength}${(char)0x00}${ipAddress}${(char)macAddressLength}${(char)0x00}${macAddress}${(char)remoteNameLength}${(char)0x00}${remoteName}"
	def authenticationMessageLength = authenticationMessage.getBytes().size()
    
    def authenticationPacket = "${(char)0x00}${(char)appStringLength}${(char)0x00}${appString}${(char)authenticationMessageLength}${(char)0x00}${authenticationMessage}"

	// If our initial run, just send the authentication packet so the prompt appears on screen
	if (key == "AUTHENTICATE") {
	    sendHubCommand(new physicalgraph.device.HubAction(authenticationPacket, physicalgraph.device.Protocol.LAN, "${ipAddressHex}:D6D8"))
    } else {
        // Build the command we will send to the Samsung TV
        def command = "KEY_${key}".encodeAsBase64().toString()
        def commandLength = command.getBytes().size()

        def actionMessage = "${(char)0x00}${(char)0x00}${(char)0x00}${(char)commandLength}${(char)0x00}${command}"
        def actionMessageLength = actionMessage.getBytes().size()

        def actionPacket = "${(char)0x00}${(char)tvAppStringLength}${(char)0x00}${tvAppString}${(char)actionMessageLength}${(char)0x00}${actionMessage}"

        // Send both the authentication and action at the same time
        sendHubCommand(new physicalgraph.device.HubAction(authenticationPacket + actionPacket, physicalgraph.device.Protocol.LAN, "${ipAddressHex}:D6D8"))
    }
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpUSN = valueString
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpTerm = valueString
			}
		}
	}

	event
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
