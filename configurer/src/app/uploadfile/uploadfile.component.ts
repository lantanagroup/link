import {Component, Input, OnInit} from '@angular/core';
import {FileUploader} from 'ng2-file-upload';
import {ToastService} from "../toast.service";
import {EnvironmentService} from "../environment.service";

const URL = '/configurer/api/upload/';


@Component({
  selector: 'upload-file',
  templateUrl: './uploadfile.component.html',
  styleUrls: ['./uploadfile.component.css'],
})

export class UploadFileComponent implements OnInit {
  path: string = '';
  @Input() configType: string;
  public uploader: FileUploader;

  constructor(private toastr: ToastService, public envService: EnvironmentService) {

  }

  ngOnInit() {
    const path = URL + this.configType;
    this.uploader = new FileUploader({
      url: path,
      itemAlias: this.configType,
    });
    this.uploader.onAfterAddingFile = (file) => {
      file.withCredentials = false;
    };
    this.uploader.onCompleteItem = (item: any, status: any) => {
      console.log('Uploaded File Details:', item);
      this.toastr.show('File successfully uploaded!', 'File successfully uploaded!', 2000);
      window.location.reload();
    };

  }
}
