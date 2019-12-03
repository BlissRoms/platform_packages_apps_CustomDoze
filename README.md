<img src="https://i.imgur.com/0GnrwaU.png">

How to add CustomDoze to your device

Add item below to your device tree's common.mk file

# Doze
PRODUCT_PACKAGES += \
    CustomDoze


Add in the overlay to Settings Add

    <string name="config_customDozePackage">com.custom.ambient.display/com.custom.ambient.display.DozeSettings</string>
