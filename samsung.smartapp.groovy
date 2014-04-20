/**
 *  Samsung TV (When Mode Changes or App Is Pressed)
 *
 *  Author: Brad Butner
 *  Date: 2014-04-20
 *
 *  This currently only allows you to trigger actions on your TV if SmartThings detects
 *  a mode change (or you press the app). You can, of course, modify this to support other
 *  actions. The end goal is to add the TV as an actual Device on your Things page, but I haven't
 *  yet been able to figure out how to make LAN calls from a device (simple enough to do from an app).
 *
 *  PLEASE NOTE: This has only been tested on a Samsung UN50EH5300 TV. It may or may not work for you. Also,
 *  you will need to know your device's IP and MAC Address. This information should be viewable from your
 *  TV under Menu > Network > Network Status.
 */
preferences {
	section("Samsung TV Settings") {
		input "settingIpAddress", "text", title: "IP Address", required: true
		input "settingMacAddress", "text", title: "MAC Address", required: true
    input "tvCommand", "enum", title: "Perform This Command", metadata:[values:["POWEROFF","POWERON","AV1","AV2","AV3","CLOCK_DISPLAY","COMPONENT1", "COMPONENT2", "HDMI", "HDMI1", "HDM2", "HDM3", "HDMI4", "INFO", "SLEEP"]], required: true
	}
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

    subscribe(app, appTouch)
    subscribe(location, changedLocationMode)
    tvAction("AUTHENTICATE")
}

def changedLocationMode(evt) {
	log.debug "changedLocationMode: $evt"
	tvAction(tvCommand)
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	tvAction(tvCommand)
}

def parse(event) {
	log.debug "${event}"
}

private tvAction(key) {
	log.debug "Executing ${tvCommand}"

    // Standard Connection Data
    def appString = "iphone..iapp.samsung"
    def appStringLength = appString.getBytes().size()

    def tvAppString = "iphone.UN60ES8000.iapp.samsung"
    def tvAppStringLength = tvAppString.getBytes().size()

    def remoteName = "SmartThings".encodeAsBase64().toString()
    def remoteNameLength = remoteName.getBytes().size()

    // Device Connection Data
    def ipAddress = settingIpAddress.encodeAsBase64().toString()
    def ipAddressLength = ipAddress.getBytes().size()
    def ipAddressHex = settingIpAddress.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP Address (HEX): ${ipAddressHex}"

    def macAddress = settingMacAddress.replaceAll(":","").encodeAsBase64().toString()
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
