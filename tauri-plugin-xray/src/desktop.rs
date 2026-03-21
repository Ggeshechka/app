use serde::de::DeserializeOwned;
use serde_json::Value;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<Xray<R>> {
  Ok(Xray(app.clone()))
}

/// Access to the xray APIs.
pub struct Xray<R: Runtime>(AppHandle<R>);

impl<R: Runtime> Xray<R> {
  pub fn ping(&self, payload: PingRequest) -> crate::Result<PingResponse> {
    let json_str = payload.value.unwrap_or_else(|| "{}".to_string());
    
    let response_value = match serde_json::from_str::<Value>(&json_str) {
        Ok(json) => {
            let action = json["action"].as_str().unwrap_or("unknown");
            let data = json["data"].as_str().unwrap_or("");

            match action {
                "startVpn" => format!("VPN started with config: {}", data),
                "stopVpn" => "VPN stopped".to_string(),
                "getStatus" => "Disconnected".to_string(),
                _ => format!("Error: Unknown action '{}'", action),
            }
        }
        Err(e) => format!("Error parsing JSON: {}", e),
    };

    Ok(PingResponse {
      value: Some(response_value),
    })
  }
}