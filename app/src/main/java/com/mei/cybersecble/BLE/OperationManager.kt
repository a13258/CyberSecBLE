package com.mei.cybersecble.BLE

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.mei.cybersecble.API.*
import retrofit2.Call
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.experimental.and
import kotlin.experimental.or
import retrofit2.Callback

private const val GATT_MIN_MTU_SIZE = 23

private const val GATT_MAX_MTU_SIZE = 517

var auth: Boolean = false
var HRTimerenabled: Boolean = false
var prevContext: Context? = null

object ConnectionManager {

    private var listeners: MutableSet<WeakReference<ConnectionEventListener>> = mutableSetOf()

    private val deviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private var pendingOperation: BleOperationType? = null

    fun authBiostrap(btDevice: BluetoothDevice) {

        val primaryServUuid = UUID.fromString("cca31000-78c6-4785-9e45-0887d451317c")
        val bootprimaryServUuid = UUID.fromString("00060000-f8ce-11e4-abf4-0002a5d5c51b")
        val batteryprimaryServUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

        val debugCharUuid = UUID.fromString("cca3000f-78c6-4785-9e45-0887d451317c")
        val macroCharUuid = UUID.fromString("cca30006-78c6-4785-9e45-0887d451317c")
        val tempCharUuid = UUID.fromString("cca30007-78c6-4785-9e45-0887d451317c")
        val bootloaderCharUuid = UUID.fromString("00060001-f8ce-11e4-abf4-0002a5d5c51b")
        val batteryCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        val primarySer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(primaryServUuid)
        val bootprimarySer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(bootprimaryServUuid)
        val batteryprimarySer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(batteryprimaryServUuid)

        val debugChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(debugCharUuid)
        val macroChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(macroCharUuid)
        val tempChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(tempCharUuid)
        val bootloaderChar: BluetoothGattCharacteristic = bootprimarySer!!.getCharacteristic(bootloaderCharUuid)
        val batteryChar: BluetoothGattCharacteristic = batteryprimarySer!!.getCharacteristic(batteryCharUuid)


        val plogCharUuid = UUID.fromString("cca30003-78c6-4785-9e45-0887d451317c")
        val plogChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(plogCharUuid)


        enableNotifications(btDevice,debugChar)
        enableNotifications(btDevice,macroChar)
        enableNotifications(btDevice,tempChar)
        enableNotifications(btDevice,bootloaderChar)
        enableNotifications(btDevice,batteryChar)
        enableNotifications(btDevice,plogChar)

        biostrapNext(btDevice)
    }

    fun biostrapNext(btDevice: BluetoothDevice) {
        val deviceInfServUuid = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val firmwareRevisonCharUuid = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val hardwareRevisionCharUuid = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")

        val deviceInfSer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(deviceInfServUuid)
        val firmwareRevisionChar: BluetoothGattCharacteristic = deviceInfSer!!.getCharacteristic(firmwareRevisonCharUuid)
        val hardwareRevisionChar: BluetoothGattCharacteristic = deviceInfSer!!.getCharacteristic(hardwareRevisionCharUuid)

        readCharacteristic(btDevice, firmwareRevisionChar)
        readCharacteristic(btDevice, hardwareRevisionChar)

        biostrapFinal(btDevice)
    }

    fun biostrapFinal(btDevice: BluetoothDevice) {
        val primaryServUuid = UUID.fromString("cca31000-78c6-4785-9e45-0887d451317c")
        val debugCharUuid = UUID.fromString("cca3000f-78c6-4785-9e45-0887d451317c")
        val macroCharUuid = UUID.fromString("cca30006-78c6-4785-9e45-0887d451317c")
        val configCharUuid = UUID.fromString("cca30002-78c6-4785-9e45-0887d451317c")

        val primarySer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(primaryServUuid)

        val debugChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(debugCharUuid)
        val macroChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(macroCharUuid)
        val configChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(configCharUuid)

        val payload: ByteArray = byteArrayOf(0x11,0x04,0x19,0x01)

        writeCharacteristic(btDevice,debugChar,payload)

        var payload3 = "00df00fcce7362"
        var srcBytes = payload3.toByteArray()
        srcBytes = payload3.decodeHex()
        writeCharacteristic(btDevice,macroChar,srcBytes)

        payload3 = "000000ffff000000000000000000000000"
        srcBytes = payload3.toByteArray()
        srcBytes = payload3.decodeHex()
        writeCharacteristic(btDevice,configChar,srcBytes)

        val payload2: ByteArray = byteArrayOf(0x05,0x04,0x04,0x02,0x04)

        writeCharacteristic(btDevice,configChar,payload2)

        payload3 = "0900802b0000004c4f0a0a2d002c010045"
        srcBytes = payload3.toByteArray()
        srcBytes = payload3.decodeHex()
        writeCharacteristic(btDevice,configChar,srcBytes)

        val plogCharUuid = UUID.fromString("cca30003-78c6-4785-9e45-0887d451317c")
        val plogChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(plogCharUuid)

        val payload4 = byteArrayOf(0x00)
        writeCharacteristic(btDevice,plogChar,payload4)

        toast(prevContext,"Successfully authenticated!")

        val req = Request(btDevice.address,0)

        val api = RetrofitInstance.buildService(APIInterface::class.java)
        api.newUser(req).enqueue(object: Callback<com.mei.cybersecble.API.Response> {
            override fun onResponse(call: Call<com.mei.cybersecble.API.Response>,
                                    response: retrofit2.Response<com.mei.cybersecble.API.Response>) {
            }

            override fun onFailure(call: Call<com.mei.cybersecble.API.Response>?,
                                   t: Throwable?) {
                Log.d("APIError", t.toString())
            }
        })
    }

    fun authXiaomi(btDevice: BluetoothDevice) {
        val authServUuid = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb")
        val authCharUuid = UUID.fromString("00000009-0000-3512-2118-0009af100700")
        val authSer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(authServUuid)
        val authChar: BluetoothGattCharacteristic = authSer!!.getCharacteristic(authCharUuid)

        enableNotifications(btDevice,authChar)

        authChallenge(btDevice)
    }

    fun authChallenge(btDevice: BluetoothDevice) {
        val authServUuid = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb")
        val authCharUuid = UUID.fromString("00000009-0000-3512-2118-0009af100700")
        val authSer: BluetoothGattService? = deviceGattMap[btDevice]?.getService(authServUuid)
        val authChar: BluetoothGattCharacteristic = authSer!!.getCharacteristic(authCharUuid)

        val payload: ByteArray = byteArrayOf(0x82.toByte(),0x00,0x02,0x01,0x00)

        writeCharacteristic(btDevice,authChar,payload)
    }

    fun turnOnHR(gatt: BluetoothGatt) {
        val hmServUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val sensorServUuid = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")
        val hrMeasureCharUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val hmControlCharUuid = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")
        val sensorCharUuid = UUID.fromString("00000001-0000-3512-2118-0009af100700")

        val hmSer: BluetoothGattService? = gatt.getService(hmServUuid)
        val sensorSer: BluetoothGattService? = gatt.getService(sensorServUuid)

        val hmChar: BluetoothGattCharacteristic = hmSer!!.getCharacteristic(hmControlCharUuid)
        val hrChar: BluetoothGattCharacteristic = hmSer!!.getCharacteristic(hrMeasureCharUuid)
        val sensorChar: BluetoothGattCharacteristic = sensorSer!!.getCharacteristic(sensorCharUuid)

        //stop continuous heart rate measurement
        writeCharacteristic(gatt.device,hmChar, stopHeartMeasurementContinuous)

        //stop single heart rate measurement
        writeCharacteristic(gatt.device,hmChar, stopHeartMeasurementSingle)

        //enabling sensor characteristic
        writeCharacteristic(gatt.device,sensorChar, byteArrayOf(0x01,0x03,0x19))

        //enable notifications
        enableNotifications(gatt.device,hrChar)

        //start heart rate measurement
        writeCharacteristic(gatt.device,hmChar, startHeartMeasurementContinuous)

        //writing to sensor characteristic
        writeCharacteristic(gatt.device,sensorChar, byteArrayOf(0x02))

        Log.i("Ping", "HRControll")
        writeCharacteristic(gatt.device,hmChar, byteArrayOf(0x16))
    }

    fun connect(device: BluetoothDevice, context: Context) {
        prevContext = context
        if (device.isConnected()) {
            Log.d("Connect","Already connected to ${device.address}!")
        } else {
            enqueueOperation(Connect(device, context.applicationContext))
        }
    }

    fun teardownConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            enqueueOperation(Disconnect(device))
        } else {
            Log.d("Connect","Not connected to ${device.address}, cannot teardown connection!")
        }
    }

    fun readCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() && characteristic.isReadable()) {
            enqueueOperation(CharacteristicRead(device, characteristic.uuid))
        } else if (!characteristic.isReadable()) {
            Log.d("ReadChar","Attempting to read ${characteristic.uuid} that isn't readable!")
        } else if (!device.isConnected()) {
            Log.d("ReadChar","Not connected to ${device.address}, cannot perform characteristic read")
        }
    }

    fun writeCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray
    ) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> {
                Log.d("WriteChar","Characteristic ${characteristic.uuid} cannot be written to")
                return
            }
        }
        if (device.isConnected()) {
            enqueueOperation(CharacteristicWrite(device, characteristic.uuid, writeType, payload))
        } else {
            Log.d("WriteChar","Not connected to ${device.address}, cannot perform characteristic write")
        }
    }

    fun enableNotifications(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() &&
            (characteristic.isIndicatable() || characteristic.isNotifiable())
        ) {
            enqueueOperation(EnableNotifications(device, characteristic.uuid))
        } else if (!device.isConnected()) {
            Log.d("EnableNot","Not connected to ${device.address}, cannot enable notifications")
        } else if (!characteristic.isIndicatable() && !characteristic.isNotifiable()) {
            Log.d("EnableNot","Characteristic ${characteristic.uuid} doesn't support notifications/indications")
        }
    }

    fun requestMtu(device: BluetoothDevice, mtu: Int) {
        if (device.isConnected()) {
            enqueueOperation(MtuRequest(device, mtu.coerceIn(GATT_MIN_MTU_SIZE, GATT_MAX_MTU_SIZE)))
        } else {
            Log.d("RequestMTU","Not connected to ${device.address}, cannot request MTU update!")
        }
    }

    @Synchronized
    private fun enqueueOperation(operation: BleOperationType) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun signalEndOfOperation() {
        Log.d("Operation","End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    /**
     * Perform a given [BleOperationType]. All permission checks are performed before an operation
     * can be enqueued by [enqueueOperation].
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun doNextOperation() {
        if (pendingOperation != null) {
            Log.d("Operation","doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.d("Operation","Operation queue empty, returning")
            return
        }
        pendingOperation = operation

        // Handle Connect separately from other operations that require device to be connected
        if (operation is Connect) {
            with(operation) {
                Log.d("Operation","Connecting to ${device.address}")
                device.connectGatt(context, false, callback)
            }
            return
        }

        // Check BluetoothGatt availability for other operations
        val gatt = deviceGattMap[operation.device]
            ?: this@ConnectionManager.run {
                Log.d("Operation","Not connected to ${operation.device.address}! Aborting $operation operation.")
                signalEndOfOperation()
                return
            }

        // TODO: Make sure each operation ultimately leads to signalEndOfOperation()
        // TODO: Refactor this into an BleOperationType abstract or extension function
        when (operation) {
            is Disconnect -> with(operation) {
                Log.d("Operation","Disconnecting from ${device.address}")
                gatt.close()
                deviceGattMap.remove(device)
                listeners.forEach { it.get()?.onDisconnect?.invoke(device) }
                signalEndOfOperation()
            }
            is CharacteristicWrite -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    characteristic.writeType = writeType
                    characteristic.value = payload
                    gatt.writeCharacteristic(characteristic)
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $characteristicUuid to write to")
                    signalEndOfOperation()
                }
            }
            is CharacteristicRead -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    gatt.readCharacteristic(characteristic)
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $characteristicUuid to read from")
                    signalEndOfOperation()
                }
            }
            is DescriptorWrite -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    descriptor.value = payload
                    gatt.writeDescriptor(descriptor)
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $descriptorUuid to write to")
                    signalEndOfOperation()
                }
            }
            is DescriptorRead -> with(operation) {
                gatt.findDescriptor(descriptorUuid)?.let { descriptor ->
                    gatt.readDescriptor(descriptor)
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $descriptorUuid to read from")
                    signalEndOfOperation()
                }
            }
            is EnableNotifications -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
                    val payload = when {
                        characteristic.isIndicatable() ->
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        characteristic.isNotifiable() ->
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        else ->
                            error("${characteristic.uuid} doesn't support notifications/indications")
                    }

                    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                        if (!gatt.setCharacteristicNotification(characteristic, true)) {
                            Log.d("Operation","setCharacteristicNotification failed for ${characteristic.uuid}")
                            signalEndOfOperation()
                            return
                        }

                        cccDescriptor.value = payload
                        gatt.writeDescriptor(cccDescriptor)
                    } ?: this@ConnectionManager.run {
                        Log.d("Operation","${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOfOperation()
                    }
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $characteristicUuid! Failed to enable notifications.")
                    signalEndOfOperation()
                }
            }
            is DisableNotifications -> with(operation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
                    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                        if (!gatt.setCharacteristicNotification(characteristic, false)) {
                            Log.d("Operation","setCharacteristicNotification failed for ${characteristic.uuid}")
                            signalEndOfOperation()
                            return
                        }

                        cccDescriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(cccDescriptor)
                    } ?: this@ConnectionManager.run {
                        Log.d("Operation","${characteristic.uuid} doesn't contain the CCC descriptor!")
                        signalEndOfOperation()
                    }
                } ?: this@ConnectionManager.run {
                    Log.d("Operation","Cannot find $characteristicUuid! Failed to disable notifications.")
                    signalEndOfOperation()
                }
            }
            is MtuRequest -> with(operation) {
                gatt.requestMtu(mtu)
            }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("ConnectionChange","onConnectionStateChange: connected to $deviceAddress")
                    toast(prevContext,"Connected!")
                    deviceGattMap[gatt.device] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("ConnectionChange","onConnectionStateChange: disconnected from $deviceAddress")
                    toast(prevContext,"Disconnected!")
                    teardownConnection(gatt.device)
                }
            } else {
                Log.d("ConnectionChange","onConnectionStateChange: status $status encountered for $deviceAddress!")
                if (pendingOperation is Connect) {
                    signalEndOfOperation()
                }
                teardownConnection(gatt.device)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("ServicesDisc","Discovered ${services.size} services for ${device.address}.")
                    printGattTable()
                    requestMtu(device, GATT_MAX_MTU_SIZE)
                    listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(this) }
                    if (gatt.device.name.contains("Mi Smart Band")) {
                        authXiaomi(gatt.device)
                    } else if (gatt.device.name.contains("EVO")) {
                        authBiostrap(gatt.device)
                    }
                } else {
                    Log.d("ServicesDi","Service discovery failed due to status $status")
                    teardownConnection(gatt.device)
                }
            }

            if (pendingOperation is Connect) {
                signalEndOfOperation()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("MTUChanged","ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
            listeners.forEach { it.get()?.onMtuChanged?.invoke(gatt.device, mtu) }

            if (pendingOperation is MtuRequest) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d("CharRead","Read characteristic $uuid | value: ${value.toHexString()}")
                        listeners.forEach { it.get()?.onCharacteristicRead?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.d("CharRead","Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.d("CharRead","Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicRead) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d("CharWrite","Wrote to characteristic $uuid | value: ${value.toHexString()}")
                        listeners.forEach { it.get()?.onCharacteristicWrite?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.d("CharWrite","Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.d("CharWrite","Characteristic write failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicWrite) {
                signalEndOfOperation()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.d("CharChanged","Characteristic $uuid changed | value: ${value.toHexString()}")
                listeners.forEach { it.get()?.onCharacteristicChanged?.invoke(gatt.device, this) }
                val value = characteristic.value
                val str: String = byteArrayToHex(value)
                //Log.d("value", str)
                //Log.d("value0", value[0].toString())
                if (value[0] == 0x80.toByte()) {
                    val primaryServUuid = UUID.fromString("cca31000-78c6-4785-9e45-0887d451317c")
                    val plogCharUuid = UUID.fromString("cca30003-78c6-4785-9e45-0887d451317c")

                    val primarySer: BluetoothGattService? = deviceGattMap[gatt.device]?.getService(primaryServUuid)
                    val plogChar: BluetoothGattCharacteristic = primarySer!!.getCharacteristic(plogCharUuid)

                    val payload = byteArrayOf(0x00)
                    writeCharacteristic(gatt.device,plogChar,payload)
                }
                if(!auth && gatt.device.name.contains("Mi Smart Band")) {
                    if (value[0] == AUTH_RESPONSE && value[1] == AUTH_SEND_KEY && value[2] == AUTH_SUCCESS) {
                        writeCharacteristic(gatt.device,characteristic, requestAuthNumber())
                    } else if (value[0] == AUTH_RESPONSE && value[1] and 0x0f == AUTH_REQUEST_RANDOM_AUTH_NUMBER && value[2] == AUTH_SUCCESS) {
                        val eValue: ByteArray = handleAESAuth(value, getSecretKey())!!
                        val value1 = (AUTH_SEND_ENCRYPTED_AUTH_NUMBER or getCryptFlags())
                        val authFlags = getAuthFlags()
                        val responseValue = concat(byteArrayOf(value1), byteArrayOf(authFlags), eValue)
                        //val str: String = byteArrayToHex(responseValue)
                        //Log.d("response", str)
                        writeCharacteristic(gatt.device,characteristic, responseValue)
                    } else if (value[0] == AUTH_RESPONSE && value[1] and 0x0f == AUTH_SEND_ENCRYPTED_AUTH_NUMBER && value[2] == AUTH_SUCCESS) {
                        val req = Request(gatt.device.address,0)

                        val api = RetrofitInstance.buildService(APIInterface::class.java)
                        api.newUser(req).enqueue(object: Callback<com.mei.cybersecble.API.Response> {
                            override fun onResponse(call: Call<com.mei.cybersecble.API.Response>,
                                                    response: retrofit2.Response<com.mei.cybersecble.API.Response>) {
                            }

                            override fun onFailure(call: Call<com.mei.cybersecble.API.Response>?,
                                                   t: Throwable?) {
                                Log.d("APIError", t.toString())
                            }
                        })

                        toast(prevContext,"Successfully authenticated!")
                        auth = true
                        gatt.device.createBond()
                        turnOnHR(gatt)
                    } else if (value[0] == 0x10.toByte() && value[1] == 0x83.toByte() && value[2] == 0x08.toByte()) {
                        teardownConnection(gatt.device)
                        toast(prevContext,"Invalid authkey!")
                    }
                }

                if (!HRTimerenabled && characteristic.uuid==UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")) {
                    HRTimerenabled = true
                    val hmServUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
                    val hmControlCharUuid = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")

                    val hmSer: BluetoothGattService? = gatt.getService(hmServUuid)

                    val hmChar: BluetoothGattCharacteristic = hmSer!!.getCharacteristic(hmControlCharUuid)
                    Timer().scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            Log.i("Ping", "HRControll")
                            writeCharacteristic(gatt.device,hmChar, byteArrayOf(0x16))
                        }
                    }, 0, 12000)
                }

                if (characteristic.uuid==UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")) {
                    val req = Request(gatt.device.address,Integer.valueOf(str,16))

                    val api = RetrofitInstance.buildService(APIInterface::class.java)
                    api.userHRM(req).enqueue(object: Callback<com.mei.cybersecble.API.Response> {
                        override fun onResponse(call: Call<com.mei.cybersecble.API.Response>,
                                                response: retrofit2.Response<com.mei.cybersecble.API.Response>) {
                        }

                        override fun onFailure(call: Call<com.mei.cybersecble.API.Response>?,
                                               t: Throwable?) {
                            Log.d("APIError", t.toString())
                        }
                    })
                }
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d("DescRead","Read descriptor $uuid | value: ${value.toHexString()}")
                        listeners.forEach { it.get()?.onDescriptorRead?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.d("DescRead","Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.d("DescRead","Descriptor read failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is DescriptorRead) {
                signalEndOfOperation()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d("DescWrite","Wrote to descriptor $uuid | value: ${value.toHexString()}")

                        if (isCccd()) {
                            onCccdWrite(gatt, value, characteristic)
                        } else {
                            listeners.forEach { it.get()?.onDescriptorWrite?.invoke(gatt.device, this) }
                        }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.d("DescWrite","Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.d("DescWrite","Descriptor write failed for $uuid, error: $status")
                    }
                }
            }

            if (descriptor.isCccd() &&
                (pendingOperation is EnableNotifications || pendingOperation is DisableNotifications)
            ) {
                signalEndOfOperation()
            } else if (!descriptor.isCccd() && pendingOperation is DescriptorWrite) {
                signalEndOfOperation()
            }
        }

        private fun onCccdWrite(
            gatt: BluetoothGatt,
            value: ByteArray,
            characteristic: BluetoothGattCharacteristic
        ) {
            val charUuid = characteristic.uuid
            val notificationsEnabled =
                value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            val notificationsDisabled =
                value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)

            when {
                notificationsEnabled -> {
                    Log.d("CCCDWrite","Notifications or indications ENABLED on $charUuid")
                    listeners.forEach {
                        it.get()?.onNotificationsEnabled?.invoke(
                            gatt.device,
                            characteristic
                        )
                    }
                }
                notificationsDisabled -> {
                    Log.d("CCCDWrite","Notifications or indications DISABLED on $charUuid")
                    listeners.forEach {
                        it.get()?.onNotificationsDisabled?.invoke(
                            gatt.device,
                            characteristic
                        )
                    }
                }
                else -> {
                    Log.d("CCCDWrite","Unexpected value ${value.toHexString()} on CCCD of $charUuid")
                }
            }
        }
    }

    private fun BluetoothDevice.isConnected() = deviceGattMap.containsKey(this)

    fun toast(context: Context?, string: String?) {
        Handler(Looper.getMainLooper()).post {
            val toast = Toast.makeText(context, string, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER or Gravity.BOTTOM, 0, 0)
            toast.show()
        }
    }
}