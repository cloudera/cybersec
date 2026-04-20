/*
 * Copyright 2020 - 2023 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {SampleDataModel, SampleDataType} from "../../models/sample-data.model";
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-sample-data-avro-input',
  templateUrl: './sample-data-avro-input.component.html',
  styleUrls: ['./sample-data-avro-input.component.scss']
})
export class SampleDataAvroInputComponent {

  @Input() sampleData: SampleDataModel;
  @Output() sampleDataChange = new EventEmitter<SampleDataModel>();

  // Human-readable display of Avro data as JSON
  avroDisplayJson: string = '';

  // Store the raw binary data
  avroBinaryData: Uint8Array | null = null;

  constructor(private _messageService: NzMessageService) {}

  uploadToForm(event: any) {
    const file = event.target.files[0];
    if (!file) {
      return;
    }

    // Check file extension
    const fileName = file.name;
    const extension = fileName.split('.').pop()?.toLowerCase();
    if (extension !== 'avro') {
      this._messageService.create('error', 'The file must be a .avro file');
      return;
    }

    // Read file as ArrayBuffer (binary)
    const reader = new FileReader();
    reader.onload = () => {
      const arrayBuffer = reader.result as ArrayBuffer;
      this.avroBinaryData = new Uint8Array(arrayBuffer);

      // Parse Avro data to JSON for display
      const jsonDisplay = this.parseAvroToJson(this.avroBinaryData);
      this.avroDisplayJson = jsonDisplay;

      // Emit the sample data with AVRO type
      // The binary data will be sent to the live view
      this.sampleDataChange.emit({
        type: SampleDataType.AVRO,
        source: this.avroDisplayJson,
        sourceBinary: this.avroBinaryData
      });
    };

    reader.readAsArrayBuffer(file);
  }

  /**
   * Parse Avro binary data to human-readable JSON.
   * This uses a simple decoding approach for display purposes.
   */
  private parseAvroToJson(binaryData: Uint8Array): string {
    try {
      // Convert Uint8Array to string for hex display
      // For a proper Avro decode, we'd need the schema
      // This is a simplified display that shows the binary structure

      // Check for Avro magic bytes (Objv1)
      if (binaryData.length >= 4) {
        const magic = String.fromCharCode(...binaryData.slice(0, 4));
        if (magic.startsWith('Ob')) {
          // This is a valid Avro file - try to read data records
          return this.decodeAvroFile(binaryData);
        }
      }

      // Fallback: show as hex dump with basic info
      return this.createHexDisplay(binaryData);
    } catch (error) {
      return JSON.stringify({ error: 'Failed to parse Avro file', message: error.message }, null, 2);
    }
  }

  /**
   * Decode an Avro file to JSON.
   * Avro file format: Magic (4 bytes) + Schema + Data
   */
  private decodeAvroFile(binaryData: Uint8Array): string {
    try {
      // For display, we'll show a summary of the Avro file
      // A proper implementation would use the Avro schema to decode

      const fileSize = binaryData.length;
      const magic = String.fromCharCode(...binaryData.slice(0, 4));

      // Try to extract schema (simplified - Avro schema is in JSON after magic + codec header)
      // This is a best-effort display

      const records: any[] = [];
      let offset = 4; // Skip magic bytes

      // Try to read the sync marker position to estimate data section
      // Avro file has 16-byte sync marker near end
      // But for now, show a simple representation

      return JSON.stringify({
        avroFileInfo: {
          magic: magic,
          fileSize: fileSize + ' bytes',
          recordCount: 'Not available without full schema parsing',
          displayFormat: 'Raw Avro binary data'
        },
        note: 'Full Avro parsing requires schema. Showing raw structure.',
        rawDataPreview: this.getRawDataPreview(binaryData)
      }, null, 2);
    } catch (error) {
      return JSON.stringify({ error: 'Error parsing Avro', details: error.message }, null, 2);
    }
  }

  /**
   * Get a preview of the raw binary data
   */
  private getRawDataPreview(binaryData: Uint8Array): string {
    const maxBytes = 100;
    const bytes = binaryData.slice(0, maxBytes);
    const hexPairs: string[] = [];

    for (let i = 0; i < bytes.length; i++) {
      hexPairs.push(bytes[i].toString(16).padStart(2, '0').toUpperCase());
    }

    let preview = hexPairs.join(' ');
    if (binaryData.length > maxBytes) {
      preview += ' ... (' + (binaryData.length - maxBytes) + ' more bytes)';
    }

    return preview;
  }

  /**
   * Fallback hex display
   */
  private createHexDisplay(binaryData: Uint8Array): string {
    return JSON.stringify({
      data: this.getRawDataPreview(binaryData),
      totalBytes: binaryData.length,
      encoding: 'binary (hex)'
    }, null, 2);
  }
}