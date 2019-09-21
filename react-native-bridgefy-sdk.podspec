Pod::Spec.new do |s|
    s.name         = "react-native-bridgefy-sdk"
    s.version      = "0.0.1 "
    s.summary      = "React Native interface for Bridgefy"
    s.description  = <<-DESC
    React Native interface for Bridgefy.
                     DESC
  
    s.homepage     = "https://github.com/marcosrdz/react-native-bridgefy-sdk"
    s.license      = "Unlicense"
    s.author             = { "Bridgefy" => "https://bridgefy.me" }
    s.platform     = :ios
    s.platform     = :ios, "10.0"
    s.source       = { :git => "https://github.com/marcosrdz/react-native-bridgefy-sdk.git", :tag => "v#{s.version}" }
    s.source_files  = "ios/*.{h,m}"
    s.public_header_files = "ios/*.h"
    s.dependency "React"
    s.dependency "bridgefy-ios-developer"
  end