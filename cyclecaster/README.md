# CycleCaster

The CycleCaster is a CycleFlow base station using the nRF52 series Bluetooth
SoC. It can run internal timing demonstrations with static data, and is designed
to be integrated with traffic signal control systems.

## Requirements

Building this project requires:

- Nordic Semiconductor's nRF52 SDK v15.2.0 (submodule, or [link](https://developer.nordicsemi.com/nRF5_SDK/nRF5_SDK_v15.x.x/))
- GNU toolchain for ARM Cortex M ([link](https://developer.arm.com/open-source/gnu-toolchain/gnu-rm/downloads))
- nRF5x Command Line Tools ([link](http://infocenter.nordicsemi.com/index.jsp?topic=%2Fcom.nordic.infocenter.tools%2Fdita%2Ftools%2Fnrf5x_command_line_tools%2Fnrf5x_installation.html))
- GCC and GNU Make
- (Optional) SEGGER J-Link software (for debugging) ([link](https://www.segger.com/downloads/jlink/))

## Building

1. Edit the `Makefile` in `cyclecaster-nrf52/pca10040/s132/armgcc/` to reflect the SDK path (if you are using the submodule, no changes are necessary)
2. Edit `Makefile.posix` or `Makefile.windows` in `$SDK_ROOT/components/toolchain/gcc/` to reflect the version and path of the GNU ARM toolchain
4. Run `make` to test the configuration
3. Make sure `nrfjprog` is in your `$PATH`
4. Connect your nRF52-DK via USB
5. Run `make flash` to program the development kit
