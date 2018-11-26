/** @file
 *
 * @brief CycleFlow main program
 *
 * This project is based on Nordic's ble_app_beacon peripheral sample application. 
 */

/* ========================= Includes ========================= */

#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "nordic_common.h"
#include "bsp.h"
#include "nrf_soc.h"
#include "nrf_sdh.h"
#include "nrf_sdh_ble.h"
#include "ble_advdata.h"
#include "app_timer.h"
#include "nrf_pwr_mgmt.h"

#include "nrf_log.h"
#include "nrf_log_ctrl.h"
#include "nrf_log_default_backends.h"

/* ========================= Preprocessor Definitions ========================= */

#define APP_BLE_CONN_CFG_TAG    1       /**< Unique tag for identifying a BLE advertising set (see BLE_GAP_ADV_SET_COUNT_MAX) */
#define ADVERTISING_INTERVAL    MSEC_TO_UNITS(100, UNIT_0_625_MS)   /**< Advertising interval for non-connectable advertisment (min 100ms) */
#define APP_COMPANY_IDENTIFIER  0xFFFF  /**< Bluetooth GAP company identifiers (0xFFFF reserved for testing) */
#define DEAD_BEEF               0xDEADBEEF  /**< Error code for debugging stack dumps. */
#define NUM_ADV_BUFFERS         2       /**< Number of advertising buffers used */
#define APP_BLE_MAX_PAYLOAD     27      /**< Maximum payload, net of GAP headers */

#define BLE_TX_POWER            +4      /**< Transmit power of nRF52 in dBm: -40, -20, -16, -12, -8, -4, 0 (default), +3, +4 */

#define CF_MAGIC_NUMBER         0xCF    /**< Magic number to identify CycleFlow advertisements */

#define USE_HARDCODED_NAME      1       /**< Whether to use hardcoded name or get it from SD config */
#define HARDCODED_NAME          "ABCDEFGHIJKLMNOPQRSTUVWXYZZ" // Max length 27 chars

// Configuration type -- only one of the following should be set
#define STATIC_DEMO             1       /**< Hardcoded test structure static demo */
#define INTERNAL_TIMER_DEMO     0       /**< Flag for demoing using internal timer */
#define UART_DEMO               0       /**< Not yet implemented */

/* ========================= Definitions/Declarations ========================= */

static ble_gap_adv_params_t m_adv_params;   /**< Advertising parameters (passed to sd_ble_gap_adv_set_configure) */
static uint8_t              m_adv_handle = BLE_GAP_ADV_SET_HANDLE_NOT_SET;  /**< Handle to advertising instance ID */

static uint8_t              enc_adv_data_buffer1[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< First encoded advertising data buffer */
static uint8_t              enc_sr_data_buffer1[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< First encoded scan response data buffer */
static uint8_t              enc_adv_data_buffer2[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< Second encoded advertising data buffer */
static uint8_t              enc_sr_data_buffer2[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< Second encoded scan response data buffer */

static uint8_t              raw_adv_data_buffer1[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< First raw advertising data buffer */
static uint8_t              raw_sr_data_buffer1[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< First raw scan response data buffer */
#if !STATIC_DEMO
static uint8_t              raw_adv_data_buffer2[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< Second raw advertising data buffer */
static uint8_t              raw_sr_data_buffer2[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< Second raw scan response data buffer */
#endif // !STATIC_DEMO

static uint8_t              currentBuffer = 0; /**< Buffer ID currently in use (from 0 to NUM_ADV_BUFFERS) */
#if !STATIC_DEMO
static uint8_t              nextBuffer = 1; /**< Next buffer to be used (from 0 to NUM_ADV_BUFFERS) */
#endif // !STATIC_DEMO
static bool                 advertising_init = false; /**< Tracks whether BLE has been initialized */

/**
 * @brief Array to hold advertising data buffer info
 */
static ble_gap_adv_data_t  m_gap_adv_buffers[NUM_ADV_BUFFERS] = 
{
    {
        .adv_data.p_data        = enc_adv_data_buffer1,
        .adv_data.len           = sizeof(enc_adv_data_buffer1),
        .scan_rsp_data.p_data   = enc_sr_data_buffer1,
        .scan_rsp_data.len      = sizeof(enc_sr_data_buffer1)
    },
    {
        .adv_data.p_data        = enc_adv_data_buffer2,
        .adv_data.len           = sizeof(enc_adv_data_buffer2),
        .scan_rsp_data.p_data   = enc_sr_data_buffer2,
        .scan_rsp_data.len      = sizeof(enc_sr_data_buffer2)
    }
};

/**@brief Structure to hold data for each entrance of an intersection
 * 
 * //TODO: Complete documentation
 */
typedef struct EntranceData
{
    bool demandTrigger; /**< Flag for demand-triggered (1) or timer-based (0) */
    bool isGreen;  /**< Flag for light state. 0 -> light is red, 1 -> light is green. */
    uint8_t heading;    /**< Heading for this direction of travel (0-239, 255 for omnidirectional) */
    uint8_t changeTime; /**< Time to state change (0-254 seconds, 255 is infinity) */
} EntranceData;

/**@brief Structure to hold intersection data
 * 
 * //TODO: complete documentation
 */
typedef struct IntersectionData 
{
    uint8_t latitude[3];        /**< Latitude of intersection (in CycleFlow 3-byte format) */
    uint8_t longitude[3];       /**< Longitude of intersection (in CycleFlow 3-byte format) */
    uint8_t num_entrances;      /**< Number of entrances this intersection has (should be 0-6 for protocol 1.0) */
    EntranceData entrances[6];  /**< Pointer to EntranceData structures (can be up to 6 based on num_entrances) */
    char    name[BLE_GAP_ADV_SET_DATA_SIZE_MAX]; /**< Friendly name of intersection */
} IntersectionData;

#if STATIC_DEMO
static IntersectionData intersection_data = 
{
    .latitude = {0xC0, 0x98, 0x9C},
    .longitude = {0x4A, 0x2D, 0xCF},
    .num_entrances = 4,
    .entrances = 
    {
        {
            .demandTrigger = false,
            .isGreen = true,
            .heading = 0x53,
            .changeTime = 0x1E
        },
        {
            .demandTrigger = false,
            .isGreen = true,
            .heading = 0xCB,
            .changeTime = 0x1E
        },
        {
            .demandTrigger = false,
            .isGreen = false,
            .heading = 0x1E,
            .changeTime = 0x1E
        },
        {
            .demandTrigger = false,
            .isGreen = false,
            .heading = 0x97,
            .changeTime = 0x1E,
        }
    },
    
#if USE_HARDCODED_NAME
    .name = HARDCODED_NAME
#else // ! USE_HARDCODED_NAME
    .name = "UNINITIALIZED"
#endif // USE_HARDCODED_NAME
};
#endif // STATIC_DEMO

/* ========================= Functions ========================= */

/**@brief Callback function for asserts in the SoftDevice.
 *
 * @details This function will be called in case of an assert in the SoftDevice.
 *
 * @warning On assert from the SoftDevice, the system can only recover on reset.
 *
 * @param[in]   line_num   Line number of the failing ASSERT call.
 * @param[in]   file_name  File name of the failing ASSERT call.
 */
void assert_nrf_callback(uint16_t line_num, const uint8_t * p_file_name)
{
    app_error_handler(DEAD_BEEF, line_num, p_file_name);
}

/**
 * @brief Builds beacon info array from IntersectionData struct
 * 
 * @param dest Destination array of length BLE_GAP_ADV_SET_DATA_SIZE_MAX
 * @param i_data Intersection data
 * @return uint8_t Length of resulting payload
 */
static uint8_t build_beacon_info(uint8_t dest[], IntersectionData * i_data)
{
    uint8_t i;

    // zero out existing destination array
    memset(dest, 0, BLE_GAP_ADV_SET_DATA_SIZE_MAX);

    // calculate beacon length (was APP_BEACON_INFO_LENGTH)
    uint8_t beacon_info_length = 9 + (3 * i_data->num_entrances);

    if (beacon_info_length > BLE_GAP_ADV_SET_DATA_SIZE_MAX)
    {
        NRF_LOG_ERROR("BLE advertisement packet too long in build_beacon_info!");
    }

    // copy latitude and longitude
    for (i = 0; i < 3; i++) 
    {
        dest[i] = i_data->latitude[i];
        dest[i+3] = i_data->longitude[i];
    }

    // insert static bytes
    dest[6] = CF_MAGIC_NUMBER;

    // dest[7] and dest[8] are reserved

    // copy data for each entrance
    for (i = 0; i < i_data->num_entrances; i++)
    {
        uint8_t flags = 0;
        flags |= i_data->entrances[i].demandTrigger;
        flags = flags << 1;
        flags |= i_data->entrances[i].isGreen;

        dest[9+(i*3)] = flags;
        dest[10+(i*3)] = i_data->entrances[i].heading;
        dest[11+(i*3)] = i_data->entrances[i].changeTime;
    }

    return beacon_info_length;
}

/**
 * @brief Builds scan response info array from a string or char array
 * 
 * @param dest Destination array of length BLE_GAP_ADV_SET_DATA_SIZE_MAX
 * @param i_data IntersectionData containing name (null-terminated or not)
 * @return uint8_t Length of resulting payload
 */
static uint8_t build_scan_rsp_info(uint8_t dest[], IntersectionData * i_data)
{
    uint8_t i;
    uint8_t count = 0;

    // Copy each character from i_name into dest, tracking length,
    // and ignoring null terminators
    for (i = 0; i < sizeof(i_data->name)/sizeof(char); i++) 
    {
        if (i_data->name[i] == '\0')
        {
            break;
        } 
        else
        {
            dest[i] = (uint8_t) i_data->name[i];
            count++;
        }
    }

    if (count > APP_BLE_MAX_PAYLOAD)
    {
        NRF_LOG_ERROR("BLE scan response packet too long in build_scan_rsp_info!");
    }

    return count;
}

/**@brief Function for starting advertising and setting Tx power level
 */
static void advertising_start(void)
{
    ret_code_t err_code;

    err_code = sd_ble_gap_adv_start(m_adv_handle, APP_BLE_CONN_CFG_TAG);
    APP_ERROR_CHECK(err_code);

    err_code = bsp_indication_set(BSP_INDICATE_ADVERTISING);
    APP_ERROR_CHECK(err_code);

    // Set transmit power
    err_code = sd_ble_gap_tx_power_set(BLE_GAP_TX_POWER_ROLE_ADV, m_adv_handle, BLE_TX_POWER);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing the BLE stack.
 *
 * @details Initializes the SoftDevice and the BLE event interrupt.
 */
static void ble_stack_init(void)
{
    ret_code_t err_code;

    err_code = nrf_sdh_enable_request();
    APP_ERROR_CHECK(err_code);

    // Configure the BLE stack using the default settings.
    // Fetch the start address of the application RAM.
    uint32_t ram_start = 0;
    err_code = nrf_sdh_ble_default_cfg_set(APP_BLE_CONN_CFG_TAG, &ram_start);
    APP_ERROR_CHECK(err_code);

    // Enable BLE stack.
    err_code = nrf_sdh_ble_enable(&ram_start);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing logging. */
static void log_init(void)
{
    ret_code_t err_code = NRF_LOG_INIT(NULL);
    APP_ERROR_CHECK(err_code);

    NRF_LOG_DEFAULT_BACKENDS_INIT();
}

/**@brief Function for initializing LEDs. */
static void leds_init(void)
{
    ret_code_t err_code = bsp_init(BSP_INIT_LEDS, NULL);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing timers. */
static void timers_init(void)
{
    ret_code_t err_code = app_timer_init();
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing power management. */
static void power_management_init(void)
{
    ret_code_t err_code;
    err_code = nrf_pwr_mgmt_init();
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling the idle state (main loop).
 *
 * @details If there is no pending log operation, then sleep until next the next event occurs.
 */
static void idle_state_handle(void)
{
    if (NRF_LOG_PROCESS() == false)
    {
        nrf_pwr_mgmt_run();
    }
}

/**
 * @brief //TODO: finish documentation
 * 
 * @param adv_frame_payload 
 * @param adv_frame_payload_length 
 * @param sr_frame_payload 
 * @param sr_frame_payload_length 
 * @param gap_pointers 
 */
static void advertising_setup(uint8_t * adv_frame_payload, uint16_t * adv_frame_payload_length,
                                uint8_t * sr_frame_payload, uint16_t * sr_frame_payload_length,
                                ble_gap_adv_data_t * gap_pointers)
{
    uint32_t      err_code;
    ble_advdata_t advdata; // advertising data structure
    ble_advdata_t srdata; // scan response data structure
    
    // build payload of advertisement frame
    ble_advdata_manuf_data_t        adv_payload;
    adv_payload.company_identifier  = APP_COMPANY_IDENTIFIER;
    adv_payload.data.p_data         = adv_frame_payload;
    adv_payload.data.size           = *adv_frame_payload_length;

    // build encapsulating payload
    memset(&advdata, 0, sizeof(advdata));
    advdata.name_type             = BLE_ADVDATA_NO_NAME; // data doesn't contain a name
    advdata.p_manuf_specific_data = &adv_payload;

    // -------------

    // build payload of scan response frame
    ble_advdata_manuf_data_t        sr_payload;
    sr_payload.company_identifier   = APP_COMPANY_IDENTIFIER;
    sr_payload.data.p_data          = sr_frame_payload;
    sr_payload.data.size            = *sr_frame_payload_length;

    // build encapsulating payload
    memset(&srdata, 0, sizeof(srdata));
    srdata.name_type                = BLE_ADVDATA_NO_NAME; // data doesn't contain a name
    srdata.p_manuf_specific_data    = &sr_payload;

    // encode adv_data
    err_code = ble_advdata_encode(&advdata, gap_pointers->adv_data.p_data, &gap_pointers->adv_data.len);
    APP_ERROR_CHECK(err_code);

    // encode scan_rsp_data
    err_code = ble_advdata_encode(&srdata, gap_pointers->scan_rsp_data.p_data, &gap_pointers->scan_rsp_data.len);
    APP_ERROR_CHECK(err_code);

    // configure adv_data
    if (!advertising_init) 
    {
        // Initialize advertising parameters (used when starting advertising).
        memset(&m_adv_params, 0, sizeof(m_adv_params));
        m_adv_params.properties.type = BLE_GAP_ADV_TYPE_NONCONNECTABLE_SCANNABLE_UNDIRECTED;
        m_adv_params.p_peer_addr     = NULL;    // Undirected advertisement.
        m_adv_params.filter_policy   = BLE_GAP_ADV_FP_ANY;
        m_adv_params.interval        = ADVERTISING_INTERVAL;
        m_adv_params.duration        = 0;       // Never time out.

        // Only need to set the advertising parameters on first advertisement setup
        err_code = sd_ble_gap_adv_set_configure(&m_adv_handle, gap_pointers, &m_adv_params);
        APP_ERROR_CHECK(err_code);

        advertising_init = true;
    }
    else
    {
        err_code = sd_ble_gap_adv_set_configure(&m_adv_handle, gap_pointers, NULL);
        APP_ERROR_CHECK(err_code);
    }
}

/**
 * @brief Function for application main entry.
 */
int main(void)
{
    // Initialize peripherals
    log_init();
    timers_init();
    leds_init();
    power_management_init();
    ble_stack_init();
    // TODO: initialize UART

#if STATIC_DEMO
    // If the test structure is used, the information is static (i.e. no double-buffer
    // or timer incrementing happens).
    uint16_t adv_payload_len, sr_payload_len;
    adv_payload_len = build_beacon_info(raw_adv_data_buffer1, &intersection_data);
    sr_payload_len = build_scan_rsp_info(raw_sr_data_buffer1, &intersection_data);
    advertising_setup(raw_adv_data_buffer1, &adv_payload_len,
                        raw_sr_data_buffer1, &sr_payload_len,
                        &m_gap_adv_buffers[currentBuffer]);
#elif INTERNAL_TIMER_DEMO
    
#endif

    // Start execution.
    NRF_LOG_INFO("Beacon started.");
    advertising_start();

    // Enter main loop.
    for (;;)
    {
        idle_state_handle();
    }
}
