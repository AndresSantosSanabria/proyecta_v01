import { useEffect, useMemo, useRef, useState } from 'react';
import {\r
  BadgeCheck,\r
  Ban,\r
  CircleAlert,\r
  Clock3,\r
  Briefcase,\r
  Pencil,\r
  Plus,\r
  RefreshCw,\r
  Save,\r
  Search,\r
  ShieldCheck,\r
  X,\r
  Users,\r
} from 'lucide-react';\r
import { useAuthContext } from '../../context/AuthContext';\r
import projectService from '../../services/projectService';\r
import securityService from '../../services/securityService';\r
import './SecurityConfigPage.css';\r
\r
const SECURITY_TABS = {\r
  USERS: 'usuarios',\r
  ROLES: 'roles',\r
  PARAMETERS: 'parametros',\r
  ASSIGNMENTS: 'asignaciones',\r
};\r
\r
const emptyUserForm = {\r
  username: '',\r
  nombre: '',\r
  correo: '',\r
  dependencia: '',\r
  rol: '',\r
  activo: true,\r
};\r
\r
const emptyRoleForm = {\r
  codigo: '',\r
  nombre: '',\r
  descripcion: '',\r
  transversal: false,\r
  activo: true,\r
};\r
\r
const emptyParameterForm = {\r
  key: '',\r
  value: '',\r
  descripcion: '',\r
};\r
\r
const ACTION_ORDER = [\r
  'VER',\r
  'LISTAR',\r
  'CONSULTAR',\r
  'DESCARGAR',\r
  'CREAR',\r
  'REGISTRAR',\r
  'CARGAR',\r
  'SUBIR',\r
  'EDITAR',\r
  'ACTUALIZAR',\r
  'MODIFICAR',\r
  'APROBAR',\r
  'CERRAR',\r
  'ELIMINAR',\r
  'CONFIGURAR',\r
];\r
\r
const roleLabels = {\r
  PROYECTO: 'Gestion de Proyectos',\r
  ENTREGABLE: 'Entregables',\r
// MISSING LINE 73
// MISSING LINE 74
// MISSING LINE 75
// MISSING LINE 76
// MISSING LINE 77
// MISSING LINE 78
// MISSING LINE 79
// MISSING LINE 80
// MISSING LINE 81
// MISSING LINE 82
// MISSING LINE 83
// MISSING LINE 84
// MISSING LINE 85
// MISSING LINE 86
// MISSING LINE 87
// MISSING LINE 88
// MISSING LINE 89
// MISSING LINE 90
// MISSING LINE 91
// MISSING LINE 92
// MISSING LINE 93
// MISSING LINE 94
// MISSING LINE 95
// MISSING LINE 96
// MISSING LINE 97
// MISSING LINE 98
// MISSING LINE 99
// MISSING LINE 100
// MISSING LINE 101
// MISSING LINE 102
// MISSING LINE 103
// MISSING LINE 104
// MISSING LINE 105
// MISSING LINE 106
// MISSING LINE 107
// MISSING LINE 108
// MISSING LINE 109
// MISSING LINE 110
// MISSING LINE 111
// MISSING LINE 112
// MISSING LINE 113
// MISSING LINE 114
// MISSING LINE 115
// MISSING LINE 116
// MISSING LINE 117
// MISSING LINE 118
// MISSING LINE 119
// MISSING LINE 120
// MISSING LINE 121
// MISSING LINE 122
// MISSING LINE 123
// MISSING LINE 124
// MISSING LINE 125
// MISSING LINE 126
// MISSING LINE 127
// MISSING LINE 128
// MISSING LINE 129
// MISSING LINE 130
// MISSING LINE 131
// MISSING LINE 132
// MISSING LINE 133
// MISSING LINE 134
// MISSING LINE 135
// MISSING LINE 136
// MISSING LINE 137
// MISSING LINE 138
// MISSING LINE 139
// MISSING LINE 140
// MISSING LINE 141
// MISSING LINE 142
// MISSING LINE 143
// MISSING LINE 144
// MISSING LINE 145
// MISSING LINE 146
// MISSING LINE 147
// MISSING LINE 148
// MISSING LINE 149
// MISSING LINE 150
// MISSING LINE 151
// MISSING LINE 152
// MISSING LINE 153
// MISSING LINE 154
// MISSING LINE 155
// MISSING LINE 156
// MISSING LINE 157
// MISSING LINE 158
// MISSING LINE 159
// MISSING LINE 160
// MISSING LINE 161
// MISSING LINE 162
// MISSING LINE 163
// MISSING LINE 164
// MISSING LINE 165
// MISSING LINE 166
// MISSING LINE 167
// MISSING LINE 168
// MISSING LINE 169
// MISSING LINE 170
// MISSING LINE 171
// MISSING LINE 172
// MISSING LINE 173
// MISSING LINE 174
// MISSING LINE 175
// MISSING LINE 176
// MISSING LINE 177
// MISSING LINE 178
// MISSING LINE 179
// MISSING LINE 180
// MISSING LINE 181
// MISSING LINE 182
// MISSING LINE 183
// MISSING LINE 184
// MISSING LINE 185
// MISSING LINE 186
// MISSING LINE 187
// MISSING LINE 188
// MISSING LINE 189
// MISSING LINE 190
// MISSING LINE 191
// MISSING LINE 192
// MISSING LINE 193
// MISSING LINE 194
// MISSING LINE 195
// MISSING LINE 196
// MISSING LINE 197
// MISSING LINE 198
// MISSING LINE 199
// MISSING LINE 200
// MISSING LINE 201
// MISSING LINE 202
// MISSING LINE 203
// MISSING LINE 204
// MISSING LINE 205
// MISSING LINE 206
// MISSING LINE 207
// MISSING LINE 208
// MISSING LINE 209
// MISSING LINE 210
// MISSING LINE 211
// MISSING LINE 212
// MISSING LINE 213
// MISSING LINE 214
// MISSING LINE 215
// MISSING LINE 216
// MISSING LINE 217
// MISSING LINE 218
// MISSING LINE 219
// MISSING LINE 220
// MISSING LINE 221
// MISSING LINE 222
// MISSING LINE 223
// MISSING LINE 224
// MISSING LINE 225
// MISSING LINE 226
// MISSING LINE 227
// MISSING LINE 228
// MISSING LINE 229
// MISSING LINE 230
// MISSING LINE 231
// MISSING LINE 232
// MISSING LINE 233
// MISSING LINE 234
// MISSING LINE 235
// MISSING LINE 236
// MISSING LINE 237
// MISSING LINE 238
// MISSING LINE 239
// MISSING LINE 240
// MISSING LINE 241
// MISSING LINE 242
// MISSING LINE 243
// MISSING LINE 244
// MISSING LINE 245
// MISSING LINE 246
// MISSING LINE 247
// MISSING LINE 248
// MISSING LINE 249
// MISSING LINE 250
// MISSING LINE 251
// MISSING LINE 252
// MISSING LINE 253
// MISSING LINE 254
// MISSING LINE 255
// MISSING LINE 256
// MISSING LINE 257
// MISSING LINE 258
// MISSING LINE 259
// MISSING LINE 260
// MISSING LINE 261
// MISSING LINE 262
// MISSING LINE 263
// MISSING LINE 264
// MISSING LINE 265
// MISSING LINE 266
// MISSING LINE 267
// MISSING LINE 268
// MISSING LINE 269
// MISSING LINE 270
// MISSING LINE 271
// MISSING LINE 272
// MISSING LINE 273
// MISSING LINE 274
// MISSING LINE 275
// MISSING LINE 276
// MISSING LINE 277
// MISSING LINE 278
// MISSING LINE 279
// MISSING LINE 280
// MISSING LINE 281
// MISSING LINE 282
// MISSING LINE 283
// MISSING LINE 284
// MISSING LINE 285
// MISSING LINE 286
// MISSING LINE 287
// MISSING LINE 288
// MISSING LINE 289
// MISSING LINE 290
// MISSING LINE 291
// MISSING LINE 292
// MISSING LINE 293
// MISSING LINE 294
// MISSING LINE 295
// MISSING LINE 296
// MISSING LINE 297
// MISSING LINE 298
// MISSING LINE 299
// MISSING LINE 300
// MISSING LINE 301
// MISSING LINE 302
// MISSING LINE 303
// MISSING LINE 304
// MISSING LINE 305
// MISSING LINE 306
// MISSING LINE 307
// MISSING LINE 308
// MISSING LINE 309
// MISSING LINE 310
// MISSING LINE 311
// MISSING LINE 312
// MISSING LINE 313
// MISSING LINE 314
// MISSING LINE 315
// MISSING LINE 316
// MISSING LINE 317
// MISSING LINE 318
// MISSING LINE 319
// MISSING LINE 320
// MISSING LINE 321
// MISSING LINE 322
// MISSING LINE 323
// MISSING LINE 324
// MISSING LINE 325
// MISSING LINE 326
// MISSING LINE 327
// MISSING LINE 328
// MISSING LINE 329
// MISSING LINE 330
// MISSING LINE 331
// MISSING LINE 332
// MISSING LINE 333
// MISSING LINE 334
// MISSING LINE 335
// MISSING LINE 336
// MISSING LINE 337
// MISSING LINE 338
// MISSING LINE 339
// MISSING LINE 340
// MISSING LINE 341
// MISSING LINE 342
// MISSING LINE 343
// MISSING LINE 344
// MISSING LINE 345
// MISSING LINE 346
// MISSING LINE 347
// MISSING LINE 348
// MISSING LINE 349
// MISSING LINE 350
// MISSING LINE 351
// MISSING LINE 352
// MISSING LINE 353
// MISSING LINE 354
// MISSING LINE 355
// MISSING LINE 356
// MISSING LINE 357
// MISSING LINE 358
// MISSING LINE 359
// MISSING LINE 360
// MISSING LINE 361
// MISSING LINE 362
// MISSING LINE 363
// MISSING LINE 364
// MISSING LINE 365
// MISSING LINE 366
// MISSING LINE 367
// MISSING LINE 368
// MISSING LINE 369
// MISSING LINE 370
// MISSING LINE 371
// MISSING LINE 372
// MISSING LINE 373
// MISSING LINE 374
// MISSING LINE 375
// MISSING LINE 376
// MISSING LINE 377
// MISSING LINE 378
// MISSING LINE 379
// MISSING LINE 380
// MISSING LINE 381
// MISSING LINE 382
// MISSING LINE 383
// MISSING LINE 384
// MISSING LINE 385
// MISSING LINE 386
// MISSING LINE 387
// MISSING LINE 388
// MISSING LINE 389
// MISSING LINE 390
// MISSING LINE 391
// MISSING LINE 392
// MISSING LINE 393
// MISSING LINE 394
// MISSING LINE 395
// MISSING LINE 396
// MISSING LINE 397
// MISSING LINE 398
// MISSING LINE 399
// MISSING LINE 400
// MISSING LINE 401
// MISSING LINE 402
// MISSING LINE 403
// MISSING LINE 404
// MISSING LINE 405
// MISSING LINE 406
// MISSING LINE 407
// MISSING LINE 408
// MISSING LINE 409
// MISSING LINE 410
// MISSING LINE 411
// MISSING LINE 412
// MISSING LINE 413
// MISSING LINE 414
// MISSING LINE 415
// MISSING LINE 416
// MISSING LINE 417
// MISSING LINE 418
// MISSING LINE 419
// MISSING LINE 420
// MISSING LINE 421
// MISSING LINE 422
// MISSING LINE 423
// MISSING LINE 424
// MISSING LINE 425
// MISSING LINE 426
// MISSING LINE 427
// MISSING LINE 428
// MISSING LINE 429
// MISSING LINE 430
// MISSING LINE 431
// MISSING LINE 432
// MISSING LINE 433
// MISSING LINE 434
// MISSING LINE 435
// MISSING LINE 436
// MISSING LINE 437
// MISSING LINE 438
// MISSING LINE 439
// MISSING LINE 440
// MISSING LINE 441
// MISSING LINE 442
// MISSING LINE 443
// MISSING LINE 444
// MISSING LINE 445
// MISSING LINE 446
// MISSING LINE 447
// MISSING LINE 448
// MISSING LINE 449
// MISSING LINE 450
// MISSING LINE 451
// MISSING LINE 452
// MISSING LINE 453
// MISSING LINE 454
// MISSING LINE 455
// MISSING LINE 456
// MISSING LINE 457
// MISSING LINE 458
// MISSING LINE 459
// MISSING LINE 460
// MISSING LINE 461
// MISSING LINE 462
// MISSING LINE 463
// MISSING LINE 464
// MISSING LINE 465
// MISSING LINE 466
// MISSING LINE 467
// MISSING LINE 468
// MISSING LINE 469
// MISSING LINE 470
// MISSING LINE 471
// MISSING LINE 472
// MISSING LINE 473
// MISSING LINE 474
// MISSING LINE 475
// MISSING LINE 476
// MISSING LINE 477
// MISSING LINE 478
// MISSING LINE 479
// MISSING LINE 480
// MISSING LINE 481
// MISSING LINE 482
// MISSING LINE 483
// MISSING LINE 484
// MISSING LINE 485
// MISSING LINE 486
// MISSING LINE 487
// MISSING LINE 488
// MISSING LINE 489
// MISSING LINE 490
// MISSING LINE 491
// MISSING LINE 492
// MISSING LINE 493
// MISSING LINE 494
// MISSING LINE 495
// MISSING LINE 496
// MISSING LINE 497
// MISSING LINE 498
// MISSING LINE 499
// MISSING LINE 500
// MISSING LINE 501
// MISSING LINE 502
// MISSING LINE 503
// MISSING LINE 504
// MISSING LINE 505
// MISSING LINE 506
// MISSING LINE 507
// MISSING LINE 508
// MISSING LINE 509
// MISSING LINE 510
// MISSING LINE 511
// MISSING LINE 512
// MISSING LINE 513
// MISSING LINE 514
// MISSING LINE 515
// MISSING LINE 516
// MISSING LINE 517
// MISSING LINE 518
// MISSING LINE 519
// MISSING LINE 520
// MISSING LINE 521
// MISSING LINE 522
// MISSING LINE 523
// MISSING LINE 524
// MISSING LINE 525
// MISSING LINE 526
// MISSING LINE 527
// MISSING LINE 528
// MISSING LINE 529
// MISSING LINE 530
// MISSING LINE 531
// MISSING LINE 532
// MISSING LINE 533
// MISSING LINE 534
// MISSING LINE 535
// MISSING LINE 536
// MISSING LINE 537
// MISSING LINE 538
// MISSING LINE 539
// MISSING LINE 540
// MISSING LINE 541
// MISSING LINE 542
// MISSING LINE 543
// MISSING LINE 544
// MISSING LINE 545
// MISSING LINE 546
// MISSING LINE 547
// MISSING LINE 548
// MISSING LINE 549
// MISSING LINE 550
// MISSING LINE 551
// MISSING LINE 552
// MISSING LINE 553
// MISSING LINE 554
// MISSING LINE 555
// MISSING LINE 556
// MISSING LINE 557
// MISSING LINE 558
// MISSING LINE 559
// MISSING LINE 560
// MISSING LINE 561
// MISSING LINE 562
// MISSING LINE 563
// MISSING LINE 564
// MISSING LINE 565
// MISSING LINE 566
// MISSING LINE 567
// MISSING LINE 568
// MISSING LINE 569
// MISSING LINE 570
// MISSING LINE 571
// MISSING LINE 572
// MISSING LINE 573
// MISSING LINE 574
// MISSING LINE 575
// MISSING LINE 576
// MISSING LINE 577
// MISSING LINE 578
// MISSING LINE 579
// MISSING LINE 580
// MISSING LINE 581
// MISSING LINE 582
// MISSING LINE 583
// MISSING LINE 584
// MISSING LINE 585
// MISSING LINE 586
// MISSING LINE 587
// MISSING LINE 588
// MISSING LINE 589
// MISSING LINE 590
// MISSING LINE 591
// MISSING LINE 592
// MISSING LINE 593
// MISSING LINE 594
// MISSING LINE 595
// MISSING LINE 596
// MISSING LINE 597
// MISSING LINE 598
// MISSING LINE 599
// MISSING LINE 600
// MISSING LINE 601
// MISSING LINE 602
// MISSING LINE 603
// MISSING LINE 604
// MISSING LINE 605
// MISSING LINE 606
// MISSING LINE 607
// MISSING LINE 608
// MISSING LINE 609
// MISSING LINE 610
// MISSING LINE 611
// MISSING LINE 612
// MISSING LINE 613
// MISSING LINE 614
// MISSING LINE 615
// MISSING LINE 616
// MISSING LINE 617
// MISSING LINE 618
// MISSING LINE 619
// MISSING LINE 620
// MISSING LINE 621
// MISSING LINE 622
// MISSING LINE 623
// MISSING LINE 624
// MISSING LINE 625
// MISSING LINE 626
// MISSING LINE 627
// MISSING LINE 628
// MISSING LINE 629
// MISSING LINE 630
// MISSING LINE 631
// MISSING LINE 632
// MISSING LINE 633
// MISSING LINE 634
// MISSING LINE 635
// MISSING LINE 636
// MISSING LINE 637
// MISSING LINE 638
// MISSING LINE 639
// MISSING LINE 640
// MISSING LINE 641
// MISSING LINE 642
// MISSING LINE 643
// MISSING LINE 644
// MISSING LINE 645
// MISSING LINE 646
// MISSING LINE 647
// MISSING LINE 648
// MISSING LINE 649
// MISSING LINE 650
// MISSING LINE 651
// MISSING LINE 652
// MISSING LINE 653
// MISSING LINE 654
// MISSING LINE 655
// MISSING LINE 656
// MISSING LINE 657
// MISSING LINE 658
// MISSING LINE 659
// MISSING LINE 660
// MISSING LINE 661
// MISSING LINE 662
// MISSING LINE 663
// MISSING LINE 664
// MISSING LINE 665
// MISSING LINE 666
// MISSING LINE 667
// MISSING LINE 668
// MISSING LINE 669
// MISSING LINE 670
// MISSING LINE 671
// MISSING LINE 672
// MISSING LINE 673
// MISSING LINE 674
// MISSING LINE 675
// MISSING LINE 676
// MISSING LINE 677
// MISSING LINE 678
// MISSING LINE 679
// MISSING LINE 680
// MISSING LINE 681
// MISSING LINE 682
// MISSING LINE 683
// MISSING LINE 684
// MISSING LINE 685
// MISSING LINE 686
// MISSING LINE 687
// MISSING LINE 688
// MISSING LINE 689
// MISSING LINE 690
// MISSING LINE 691
// MISSING LINE 692
// MISSING LINE 693
// MISSING LINE 694
// MISSING LINE 695
// MISSING LINE 696
// MISSING LINE 697
// MISSING LINE 698
// MISSING LINE 699
// MISSING LINE 700
// MISSING LINE 701
// MISSING LINE 702
// MISSING LINE 703
// MISSING LINE 704
// MISSING LINE 705
// MISSING LINE 706
// MISSING LINE 707
// MISSING LINE 708
// MISSING LINE 709
// MISSING LINE 710
// MISSING LINE 711
// MISSING LINE 712
// MISSING LINE 713
// MISSING LINE 714
// MISSING LINE 715
// MISSING LINE 716
// MISSING LINE 717
// MISSING LINE 718
// MISSING LINE 719
// MISSING LINE 720
// MISSING LINE 721
// MISSING LINE 722
// MISSING LINE 723
// MISSING LINE 724
// MISSING LINE 725
// MISSING LINE 726
// MISSING LINE 727
// MISSING LINE 728
// MISSING LINE 729
// MISSING LINE 730
// MISSING LINE 731
// MISSING LINE 732
// MISSING LINE 733
// MISSING LINE 734
// MISSING LINE 735
// MISSING LINE 736
// MISSING LINE 737
// MISSING LINE 738
// MISSING LINE 739
// MISSING LINE 740
// MISSING LINE 741
// MISSING LINE 742
// MISSING LINE 743
// MISSING LINE 744
// MISSING LINE 745
// MISSING LINE 746
// MISSING LINE 747
\r
  const handleDeleteRole = async () => {\r
    if (!selectedRoleCode || creatingRole) return;\r
\r
    if (!canConfigure) {\r
      setError('No tienes permisos para desactivar roles.');\r
      return;\r
    }\r
\r
    try {\r
      setSavingRole(true);\r
      setError('');\r
      await securityService.deleteRole(selectedRoleCode);\r
      setNotice('El rol se desactivo correctamente.');\r
      setCreatingRole(false);\r
      setRoleEditorOpen(false);\r
      setRoleTemplateCode('');\r
      setSelectedRoleCode('');\r
      setRoleForm(emptyRoleForm);\r
      setRoleDraftPermissions(new Set());\r
      await loadData(userSearch);\r
    } catch (deleteError) {\r
      console.error('Error desactivando rol:', deleteError);\r
      setError('No fue posible desactivar el rol.');\r
    } finally {\r
      setSavingRole(false);\r
    }\r
  };\r
\r
  const handleCancelRoleEdit = () => {\r
    setCreatingRole(false);\r
    setRoleEditorOpen(false);\r
    setRoleTemplateCode('');\r
\r
    if (selectedRole) {\r
      setRoleForm({\r
        codigo: selectedRole.codigo || '',\r
        nombre: selectedRole.nombre || '',\r
        descripcion: selectedRole.descripcion || '',\r
        transversal: Boolean(selectedRole.transversal),\r
        activo: Boolean(selectedRole.activo),\r
      });\r
      setRoleDraftPermissions(new Set((selectedRole.permisos || []).map((permiso) => permiso.codigo)));\r
      return;\r
    }\r
\r
    setRoleForm(emptyRoleForm);\r
    setRoleDraftPermissions(new Set());\r
  };\r
\r
  const handleSelectParameter = (parameter) => {\r
    setCreatingParameter(false);\r
    setParameterEditorOpen(true);\r
    setSelectedParameterKey(parameter.key);\r
    setParameterForm({\r
      key: parameter.key || '',\r
      value: parameter.value || '',\r
      descripcion: parameter.descripcion || '',\r
    });\r
    setActiveSection(SECURITY_TABS.PARAMETERS);\r
  };\r
\r
  const handleNewParameter = () => {\r
    setCreatingParameter(true);\r
    setParameterEditorOpen(true);\r
    setSelectedParameterKey('');\r
    setParameterForm(emptyParameterForm);\r
    setActiveSection(SECURITY_TABS.PARAMETERS);\r
  };\r
\r
  const handleCancelParameterEdit = () => {\r
    setCreatingParameter(false);\r
    setParameterEditorOpen(false);\r
\r
    if (selectedParameter) {\r
      setParameterForm({\r
        key: selectedParameter.key || '',\r
        value: selectedParameter.value || '',\r
        descripcion: selectedParameter.descripcion || '',\r
      });\r
      return;\r
    }\r
\r
    setParameterForm(emptyParameterForm);\r
  };\r
\r
  const handleSaveParameter = async (event) => {\r
    event.preventDefault();\r
\r
    if (!canManageSystemParameters) {\r
      setError('No tienes permisos para modificar parametros.');\r
      return;\r
    }\r
\r
    if (!parameterForm.key.trim() || !parameterForm.value.trim()) {\r
      setError('La clave y el valor del parametro son obligatorios.');\r
      return;\r
    }\r
// MISSING LINE 846
// MISSING LINE 847
// MISSING LINE 848
// MISSING LINE 849
// MISSING LINE 850
// MISSING LINE 851
// MISSING LINE 852
// MISSING LINE 853
// MISSING LINE 854
// MISSING LINE 855
// MISSING LINE 856
// MISSING LINE 857
// MISSING LINE 858
// MISSING LINE 859
// MISSING LINE 860
// MISSING LINE 861
// MISSING LINE 862
// MISSING LINE 863
// MISSING LINE 864
// MISSING LINE 865
// MISSING LINE 866
// MISSING LINE 867
// MISSING LINE 868
// MISSING LINE 869
// MISSING LINE 870
// MISSING LINE 871
// MISSING LINE 872
// MISSING LINE 873
// MISSING LINE 874
// MISSING LINE 875
// MISSING LINE 876
// MISSING LINE 877
// MISSING LINE 878
// MISSING LINE 879
// MISSING LINE 880
// MISSING LINE 881
// MISSING LINE 882
// MISSING LINE 883
// MISSING LINE 884
// MISSING LINE 885
// MISSING LINE 886
// MISSING LINE 887
// MISSING LINE 888
// MISSING LINE 889
// MISSING LINE 890
// MISSING LINE 891
// MISSING LINE 892
// MISSING LINE 893
// MISSING LINE 894
// MISSING LINE 895
// MISSING LINE 896
// MISSING LINE 897
// MISSING LINE 898
// MISSING LINE 899
// MISSING LINE 900
// MISSING LINE 901
// MISSING LINE 902
// MISSING LINE 903
// MISSING LINE 904
// MISSING LINE 905
// MISSING LINE 906
// MISSING LINE 907
// MISSING LINE 908
// MISSING LINE 909
// MISSING LINE 910
// MISSING LINE 911
// MISSING LINE 912
// MISSING LINE 913
// MISSING LINE 914
// MISSING LINE 915
// MISSING LINE 916
// MISSING LINE 917
// MISSING LINE 918
// MISSING LINE 919
// MISSING LINE 920
// MISSING LINE 921
// MISSING LINE 922
// MISSING LINE 923
// MISSING LINE 924
// MISSING LINE 925
// MISSING LINE 926
// MISSING LINE 927
// MISSING LINE 928
// MISSING LINE 929
// MISSING LINE 930
// MISSING LINE 931
// MISSING LINE 932
// MISSING LINE 933
// MISSING LINE 934
// MISSING LINE 935
// MISSING LINE 936
// MISSING LINE 937
// MISSING LINE 938
// MISSING LINE 939
// MISSING LINE 940
// MISSING LINE 941
// MISSING LINE 942
// MISSING LINE 943
// MISSING LINE 944
// MISSING LINE 945
// MISSING LINE 946
// MISSING LINE 947
// MISSING LINE 948
// MISSING LINE 949
// MISSING LINE 950
// MISSING LINE 951
// MISSING LINE 952
// MISSING LINE 953
// MISSING LINE 954
// MISSING LINE 955
// MISSING LINE 956
// MISSING LINE 957
// MISSING LINE 958
// MISSING LINE 959
// MISSING LINE 960
// MISSING LINE 961
// MISSING LINE 962
// MISSING LINE 963
// MISSING LINE 964
// MISSING LINE 965
// MISSING LINE 966
// MISSING LINE 967
// MISSING LINE 968
// MISSING LINE 969
// MISSING LINE 970
// MISSING LINE 971
// MISSING LINE 972
// MISSING LINE 973
// MISSING LINE 974
// MISSING LINE 975
// MISSING LINE 976
// MISSING LINE 977
// MISSING LINE 978
// MISSING LINE 979
// MISSING LINE 980
// MISSING LINE 981
// MISSING LINE 982
// MISSING LINE 983
// MISSING LINE 984
// MISSING LINE 985
// MISSING LINE 986
// MISSING LINE 987
// MISSING LINE 988
// MISSING LINE 989
// MISSING LINE 990
// MISSING LINE 991
// MISSING LINE 992
// MISSING LINE 993
// MISSING LINE 994
// MISSING LINE 995
// MISSING LINE 996
// MISSING LINE 997
// MISSING LINE 998
// MISSING LINE 999
// MISSING LINE 1000
// MISSING LINE 1001
// MISSING LINE 1002
// MISSING LINE 1003
// MISSING LINE 1004
// MISSING LINE 1005
// MISSING LINE 1006
// MISSING LINE 1007
// MISSING LINE 1008
// MISSING LINE 1009
// MISSING LINE 1010
// MISSING LINE 1011
// MISSING LINE 1012
// MISSING LINE 1013
// MISSING LINE 1014
// MISSING LINE 1015
// MISSING LINE 1016
// MISSING LINE 1017
// MISSING LINE 1018
// MISSING LINE 1019
// MISSING LINE 1020
// MISSING LINE 1021
// MISSING LINE 1022
// MISSING LINE 1023
// MISSING LINE 1024
// MISSING LINE 1025
// MISSING LINE 1026
// MISSING LINE 1027
// MISSING LINE 1028
// MISSING LINE 1029
// MISSING LINE 1030
// MISSING LINE 1031
// MISSING LINE 1032
// MISSING LINE 1033
// MISSING LINE 1034
// MISSING LINE 1035
// MISSING LINE 1036
// MISSING LINE 1037
// MISSING LINE 1038
// MISSING LINE 1039
// MISSING LINE 1040
// MISSING LINE 1041
// MISSING LINE 1042
// MISSING LINE 1043
// MISSING LINE 1044
// MISSING LINE 1045
// MISSING LINE 1046
// MISSING LINE 1047
// MISSING LINE 1048
// MISSING LINE 1049
      {notice && (\r
        <div className="feedback-banner success">\r
          <BadgeCheck size={18} />\r
          <span>{notice}</span>\r
        </div>\r
      )}\r
\r
      {activeSection === SECURITY_TABS.USERS && (\r
        <section className="security-workspace users-workspace">\r
          <article className="panel panel-main users-panel">\r
            <div className="panel-topbar">\r
              <div>\r
                <h2>GestiÃ³n de Usuarios</h2>\r
                <p>Control de acceso, roles y estados del personal del sistema.</p>\r
              </div>\r
\r
              <div className="panel-actions">\r
                <form className="inline-search" onSubmit={handleSearch}>\r
                  <Search size={15} />\r
                  <input\r
                    type="text"\r
                    value={userSearch}\r
                    onChange={(event) => setUserSearch(event.target.value)}\r
                    placeholder="Buscar usuario"\r
                  />\r
                </form>\r
\r
                <button type="button" className="btn-secondary" onClick={handleReload} disabled={loading}>\r
                  <RefreshCw size={16} />\r
                  Refrescar\r
                </button>\r
              </div>\r
            </div>\r
\r
            {creatingUser && (\r
              <div className="create-u
// MISSING LINE 1086
// MISSING LINE 1087
// MISSING LINE 1088
// MISSING LINE 1089
// MISSING LINE 1090
// MISSING LINE 1091
// MISSING LINE 1092
// MISSING LINE 1093
// MISSING LINE 1094
// MISSING LINE 1095
// MISSING LINE 1096
// MISSING LINE 1097
// MISSING LINE 1098
// MISSING LINE 1099
// MISSING LINE 1100
// MISSING LINE 1101
// MISSING LINE 1102
// MISSING LINE 1103
// MISSING LINE 1104
// MISSING LINE 1105
// MISSING LINE 1106
// MISSING LINE 1107
// MISSING LINE 1108
// MISSING LINE 1109
// MISSING LINE 1110
// MISSING LINE 1111
// MISSING LINE 1112
// MISSING LINE 1113
// MISSING LINE 1114
// MISSING LINE 1115
// MISSING LINE 1116
// MISSING LINE 1117
// MISSING LINE 1118
// MISSING LINE 1119
// MISSING LINE 1120
// MISSING LINE 1121
// MISSING LINE 1122
// MISSING LINE 1123
// MISSING LINE 1124
// MISSING LINE 1125
// MISSING LINE 1126
// MISSING LINE 1127
// MISSING LINE 1128
// MISSING LINE 1129
// MISSING LINE 1130
// MISSING LINE 1131
// MISSING LINE 1132
// MISSING LINE 1133
// MISSING LINE 1134
// MISSING LINE 1135
// MISSING LINE 1136
// MISSING LINE 1137
// MISSING LINE 1138
// MISSING LINE 1139
// MISSING LINE 1140
// MISSING LINE 1141
// MISSING LINE 1142
// MISSING LINE 1143
// MISSING LINE 1144
// MISSING LINE 1145
// MISSING LINE 1146
// MISSING LINE 1147
// MISSING LINE 1148
// MISSING LINE 1149
// MISSING LINE 1150
// MISSING LINE 1151
// MISSING LINE 1152
// MISSING LINE 1153
// MISSING LINE 1154
// MISSING LINE 1155
// MISSING LINE 1156
// MISSING LINE 1157
// MISSING LINE 1158
// MISSING LINE 1159
// MISSING LINE 1160
                    )}\r
                  </div>\r
                </form>\r
              </div>\r
            )}\r
\r
            {!creatingUser && (\r
              <div className="table-shell user-table-shell">\r
              <table className="data-table">\r
                <thead>\r
                  <tr>\r
                    <th>ID</th>\r
                    <th>Usuario</th>\r
                    <th>Nombre Completo</th>\r
                    <th>Rol</th>\r
                    <th>Estado</th>\r
                    <th>Ãšltimo acceso</th>\r
                    <th>Acciones</th>\r
                  </tr>\r
                </thead>\r
                <tbody>\r
                  {loading ? (\r
                    <tr>\r
                      <td colSpan={7} className="table-empty-cell">\r
                        Cargando usuarios...\r
                      </td>\r
                    </tr>\r
                  ) : usersLoadError ? (\r
                    <tr>\r
                      <td colSpan={7} className="table-empty-cell">\r
                        <div className="empty-state">\r
                          <strong>Usuarios no disponibles</strong>\r
                          <span>El backend devolviÃ³ un error al consultar la relaciÃ³n proyecta_db.usuarios. Revisa la base de datos o la migraciÃ³n de ese esquema.</span>\r
                        </div>\r
                      </td>\r
                    </tr>\r
                  ) : filteredUsers.length === 0 ? (\r
                    <tr>\r
                      <td colSpan={7} className="table-empty-cell">\r
                        No hay usuarios disponibles.\r
                      </td>\r
                    </tr>\r
                  ) : (\r
                    filteredUsers.map((user, index) => {\r
                      const isSelected = selectedUser?.username === user.username;\r
                      const initial = (user.nombre || user.username || '?')[0].toUpperCase();\r
                      const avatarColor = getAvatarColor(user.nombre || user.username);\r
                      const roleValue = getUserRoleLabel(user);\r
                      const lastAccess = formatDateTime(getUserLastAccess(user));\r
\r
                      return (\r
                        <tr\r
                          key={user.id || user.username || index}\r
                          className={isSelected ? 'selected-row' : ''}\r
                          onClick={() => handleSelectUser(user)}\r
                        >\r
                          <td className="id-cell">#{user.id || index + 1}</td>\r
                          <td>\r
                            <div className="user-chip">\r
                              <span className="user-avatar" style={{ background: avatarColor }}>\r
                                {initial}\r
                              </span>\r
                              <div>\r
                                <strong>{user.username}</strong>\r
                             
// MISSING LINE 1226
// MISSING LINE 1227
// MISSING LINE 1228
// MISSING LINE 1229
// MISSING LINE 1230
// MISSING LINE 1231
// MISSING LINE 1232
// MISSING LINE 1233
// MISSING LINE 1234
// MISSING LINE 1235
// MISSING LINE 1236
// MISSING LINE 1237
// MISSING LINE 1238
// MISSING LINE 1239
// MISSING LINE 1240
// MISSING LINE 1241
// MISSING LINE 1242
// MISSING LINE 1243
// MISSING LINE 1244
// MISSING LINE 1245
// MISSING LINE 1246
// MISSING LINE 1247
// MISSING LINE 1248
// MISSING LINE 1249
// MISSING LINE 1250
// MISSING LINE 1251
// MISSING LINE 1252
// MISSING LINE 1253
// MISSING LINE 1254
// MISSING LINE 1255
// MISSING LINE 1256
// MISSING LINE 1257
// MISSING LINE 1258
// MISSING LINE 1259
// MISSING LINE 1260
// MISSING LINE 1261
// MISSING LINE 1262
// MISSING LINE 1263
// MISSING LINE 1264
// MISSING LINE 1265
// MISSING LINE 1266
// MISSING LINE 1267
// MISSING LINE 1268
// MISSING LINE 1269
                                <label className="row-toggle" onClick={(event) => event.stopPropagation()}>\r
                                  <input type="checkbox" checked={Boolean(user.activo)} readOnly />\r
                                  <span />\r
                                </label>\r
                        const specialCodes = getBucketCodes(group, 'especiales');\r
                        const cells = [\r
                          { key: 'visualizar', codes: viewCodes },\r
                          { key: 'crear', codes: createCodes },\r
                          { key: 'editar', codes: editCodes },\r
                          { key: 'especiales', codes: specialCodes },\r
                          { key: 'visualizar', codes: viewCodes },\r
                          { key: 'crear', codes: createCodes },\r
                          { key: 'editar', codes: editCodes },\r
                          { key: 'especiales', codes: specialCodes },\r
                        ];\r
\r
                        return (\r
                          <tr key={group.key}>\r
                            <td>\r
                              <div\r
                                className="matrix-module-cell"\r
                                title={group.permissions.map((permission) => getPermissionTooltip(permission)).join(' Â· ')}\r
                              >\r
                                <span className="matrix-module-icon">\r
                                  <ShieldCheck size={15} />\r
                                </span>\r
                                <div>\r
                                  <strong>{group.label}</strong>\r
                                  <span>{group.key}</span>\r
                                </div>\r
                              </div>\r
                            </td>\r
                            {cells.map((cell) => {\r
                              const checked = isBucketChecked(cell.codes);\r
                              const cellTitle = getMatrixCellTitle(group, cell.key, cell.codes);\r
                              retur
// MISSING LINE 1306
// MISSING LINE 1307
// MISSING LINE 1308
// MISSING LINE 1309
// MISSING LINE 1310
// MISSING LINE 1311
// MISSING LINE 1312
// MISSING LINE 1313
                                        onChange={() => togglePermissionBucket(cell.codes)}\r
                                      />\r
                                      <span />\r
                                    </label>\r
                                  )}\r
                                </td>\r
                              );\r
                            })}\r
                          </tr>\r
                        );\r
                      })}\r
                    </tbody>\r
                  </table>\r
                </div>\r
\r
                <div className="form-actions sticky-actions">\r
                  <button type="button" className="btn-secondary" onClick={handleCancelRoleEdit} disabled={savingRole || savingPermissions}>\r
                    Cancelar\r
                  </button>\r
                  {selectedRole && !creatingRole && (\r
                    <button type="button" className="btn-ghost-danger" onClick={handleDeleteRole} disabled={savingRole || savingPermissions || !canConfigure}>\r
                      <Ban size={16} />\r
                      Desactivar\r
                    </button>\r
                  )}\r
                  <button type="submit" className="btn-primary" disabled={savingRole || savingPermissions || !canConfigure}>\r
                    <Save size={16} />\r
                    {savingRole || savingPermissions ? 'Guardando...' : creatingRole ? 'Crear rol' : 'Guardar rol'}\r
                  </button>\r
                </div>\r
              </section>\r
            </form>\r
          </article>\r
        </section>\r
      )}\r
    </div>\r
  );\r
};\r
// MISSING LINE 1352
// MISSING LINE 1353
// MISSING LINE 1354
// MISSING LINE 1355
// MISSING LINE 1356
// MISSING LINE 1357
// MISSING LINE 1358
// MISSING LINE 1359
// MISSING LINE 1360
// MISSING LINE 1361
// MISSING LINE 1362
// MISSING LINE 1363
// MISSING LINE 1364
// MISSING LINE 1365
// MISSING LINE 1366
// MISSING LINE 1367
// MISSING LINE 1368
// MISSING LINE 1369
// MISSING LINE 1370
// MISSING LINE 1371
// MISSING LINE 1372
// MISSING LINE 1373
// MISSING LINE 1374
// MISSING LINE 1375
// MISSING LINE 1376
// MISSING LINE 1377
// MISSING LINE 1378
// MISSING LINE 1379
// MISSING LINE 1380
// MISSING LINE 1381
// MISSING LINE 1382
// MISSING LINE 1383
// MISSING LINE 1384
// MISSING LINE 1385
// MISSING LINE 1386
// MISSING LINE 1387
// MISSING LINE 1388
// MISSING LINE 1389
// MISSING LINE 1390
// MISSING LINE 1391
// MISSING LINE 1392
// MISSING LINE 1393
// MISSING LINE 1394
// MISSING LINE 1395
// MISSING LINE 1396
// MISSING LINE 1397
// MISSING LINE 1398
// MISSING LINE 1399
// MISSING LINE 1400
// MISSING LINE 1401
// MISSING LINE 1402
// MISSING LINE 1403
// MISSING LINE 1404
// MISSING LINE 1405
// MISSING LINE 1406
// MISSING LINE 1407
// MISSING LINE 1408
// MISSING LINE 1409
// MISSING LINE 1410
// MISSING LINE 1411
// MISSING LINE 1412
// MISSING LINE 1413
// MISSING LINE 1414
// MISSING LINE 1415
// MISSING LINE 1416
// MISSING LINE 1417
// MISSING LINE 1418
// MISSING LINE 1419
// MISSING LINE 1420
// MISSING LINE 1421
// MISSING LINE 1422
// MISSING LINE 1423
// MISSING LINE 1424
// MISSING LINE 1425
// MISSING LINE 1426
// MISSING LINE 1427
// MISSING LINE 1428
// MISSING LINE 1429
// MISSING LINE 1430
// MISSING LINE 1431
// MISSING LINE 1432
// MISSING LINE 1433
// MISSING LINE 1434
// MISSING LINE 1435
// MISSING LINE 1436
// MISSING LINE 1437
// MISSING LINE 1438
// MISSING LINE 1439
// MISSING LINE 1440
// MISSING LINE 1441
// MISSING LINE 1442
// MISSING LINE 1443
// MISSING LINE 1444
// MISSING LINE 1445
// MISSING LINE 1446
// MISSING LINE 1447
// MISSING LINE 1448
// MISSING LINE 1449
                      Cancelar\r
                    </button>\r
                    {canConfigure && selectedParameter && !creatingParameter && (\r
                      <button type="button" className="btn-ghost-danger" onClick={handleDeleteParameter} disabled={savingParameter}>\r
                        <Ban size={16} />\r
                        Eliminar\r
                      </button>\r
                    )}\r
                    {canConfigure && (\r
                      <button type="submit" className="btn-primary" disabled={savingParameter}>\r
                        <Save size={16} />\r
                        {savingParameter ? 'Guardando...' : creatingParameter ? 'Crear parÃ¡metro' : 'Guardar parÃ¡metro'}\r
                      </button>\r
                    )}\r
                  </div>\r
                </form>\r
              </article>\r
            </section>\r
          )}\r
          {canConfigure && activeSection === SECURITY_TABS.ASSIGNMENTS && (
            <section className="security-workspace assignments-workspace">
              <article className="panel panel-main assignment-panel">
                <div className="panel-topbar assignments-topbar">
                  <div className="roles-header-copy">
                    <p className="security-eyebrow">ASIGNACIONES DEL SISTEMA</p>
                    <h2>Asignaci?n de Proyecto y Cargo</h2>
                    <p>
                      Administra aqu? las asignaciones entre usuarios, proyectos y cargos. Este m?dulo va separado de la gesti?n
                      de usuarios para mantener el flujo m?s claro.
                    </p>
                  </div>

                  <div className="panel-actions assignments-actions">
                    <button type="button" className="btn-secondary" onClick={() => setActiveSection(SECURITY_TABS.USERS)}>
                      <Users size={16} />
                      Volver a Usuarios
          
// MISSING LINE 1487
// MISSING LINE 1488
// MISSING LINE 1489
// MISSING LINE 1490
// MISSING LINE 1491
// MISSING LINE 1492
// MISSING LINE 1493
// MISSING LINE 1494
// MISSING LINE 1495
// MISSING LINE 1496
// MISSING LINE 1497
// MISSING LINE 1498
// MISSING LINE 1499
// MISSING LINE 1500
// MISSING LINE 1501
// MISSING LINE 1502
// MISSING LINE 1503
// MISSING LINE 1504
// MISSING LINE 1505
// MISSING LINE 1506
// MISSING LINE 1507
// MISSING LINE 1508
// MISSING LINE 1509
// MISSING LINE 1510
// MISSING LINE 1511
// MISSING LINE 1512
// MISSING LINE 1513
// MISSING LINE 1514
// MISSING LINE 1515
// MISSING LINE 1516
// MISSING LINE 1517
// MISSING LINE 1518
// MISSING LINE 1519
// MISSING LINE 1520
// MISSING LINE 1521
// MISSING LINE 1522
// MISSING LINE 1523
// MISSING LINE 1524
// MISSING LINE 1525
// MISSING LINE 1526
// MISSING LINE 1527
// MISSING LINE 1528
// MISSING LINE 1529
// MISSING LINE 1530
// MISSING LINE 1531
// MISSING LINE 1532
// MISSING LINE 1533
// MISSING LINE 1534
// MISSING LINE 1535
// MISSING LINE 1536
// MISSING LINE 1537
// MISSING LINE 1538
// MISSING LINE 1539
// MISSING LINE 1540
// MISSING LINE 1541
// MISSING LINE 1542
// MISSING LINE 1543
// MISSING LINE 1544
// MISSING LINE 1545
// MISSING LINE 1546
// MISSING LINE 1547
// MISSING LINE 1548
// MISSING LINE 1549
// MISSING LINE 1550
// MISSING LINE 1551
// MISSING LINE 1552
// MISSING LINE 1553
// MISSING LINE 1554
// MISSING LINE 1555
// MISSING LINE 1556
// MISSING LINE 1557
// MISSING LINE 1558
// MISSING LINE 1559
// MISSING LINE 1560
// MISSING LINE 1561
// MISSING LINE 1562
// MISSING LINE 1563
// MISSING LINE 1564
// MISSING LINE 1565
// MISSING LINE 1566
// MISSING LINE 1567
// MISSING LINE 1568
// MISSING LINE 1569
// MISSING LINE 1570
                        >\r
                          <option value="">\r
                            {assignmentCargos.length === 0 ? 'Sin cargos configurados' : 'Selecciona un cargo'}\r
                          </option>\r
                          {assignmentCargos.map((cargo) => (\r
                            <option key={cargo} value={cargo}>\r
                              {cargo}\r
                            </option>\r
                          ))}\r
                        </select>\r
                      </label>\r
\r
                      <div className="assignment-helper span-full">\r
                        {selectedAssignmentUser ? (\r
                          <>\r
                            <strong>{selectedAssignmentUser.nombre || selectedAssignmentUser.username}</strong>\r
                            <span>\r
                              Rol actual: {selectedAssignmentUserRole || 'Sin rol'}.\r
                              {shouldRestrictToDirectors\r
                                ? ' El cargo Director de Proyecto solo se permite para usuarios con ese rol.'\r
                                : ' Los cargos se validan contra la parametrizaci?n del sistema.'}\r
                            </span>\r
                          </>\r
                        ) : (\r
                          <>\r
                            <strong>Selecciona un usuario para comenzar</strong>\r
                            <span>\r
                              Si eliges el cargo Director de Proyecto, el selector limitar? los usuarios disponibles a ese rol.\r
                            </span>\r
                          </>\r
// MISSING LINE 1601
// MISSING LINE 1602
// MISSING LINE 1603
// MISSING LINE 1604
// MISSING LINE 1605
// MISSING LINE 1606
// MISSING LINE 1607
// MISSING LINE 1608
// MISSING LINE 1609
// MISSING LINE 1610
// MISSING LINE 1611
// MISSING LINE 1612
// MISSING LINE 1613
// MISSING LINE 1614
// MISSING LINE 1615
// MISSING LINE 1616
// MISSING LINE 1617
// MISSING LINE 1618
// MISSING LINE 1619
// MISSING LINE 1620
// MISSING LINE 1621
// MISSING LINE 1622
// MISSING LINE 1623
// MISSING LINE 1624
// MISSING LINE 1625
// MISSING LINE 1626
// MISSING LINE 1627
// MISSING LINE 1628
// MISSING LINE 1629
// MISSING LINE 1630
// MISSING LINE 1631
// MISSING LINE 1632
// MISSING LINE 1633
// MISSING LINE 1634
// MISSING LINE 1635
// MISSING LINE 1636
// MISSING LINE 1637
// MISSING LINE 1638
// MISSING LINE 1639
// MISSING LINE 1640
// MISSING LINE 1641
// MISSING LINE 1642
// MISSING LINE 1643
// MISSING LINE 1644
// MISSING LINE 1645
// MISSING LINE 1646
// MISSING LINE 1647
// MISSING LINE 1648
// MISSING LINE 1649
// MISSING LINE 1650
// MISSING LINE 1651
// MISSING LINE 1652
// MISSING LINE 1653
// MISSING LINE 1654
// MISSING LINE 1655
// MISSING LINE 1656
// MISSING LINE 1657
// MISSING LINE 1658
// MISSING LINE 1659
// MISSING LINE 1660
// MISSING LINE 1661
// MISSING LINE 1662
// MISSING LINE 1663
// MISSING LINE 1664
// MISSING LINE 1665
// MISSING LINE 1666
// MISSING LINE 1667
// MISSING LINE 1668
// MISSING LINE 1669
// MISSING LINE 1670
// MISSING LINE 1671
// MISSING LINE 1672
// MISSING LINE 1673
// MISSING LINE 1674
// MISSING LINE 1675
// MISSING LINE 1676
// MISSING LINE 1677
// MISSING LINE 1678
// MISSING LINE 1679
// MISSING LINE 1680
// MISSING LINE 1681
// MISSING LINE 1682
// MISSING LINE 1683
// MISSING LINE 1684
// MISSING LINE 1685
// MISSING LINE 1686
// MISSING LINE 1687
// MISSING LINE 1688
// MISSING LINE 1689
// MISSING LINE 1690
// MISSING LINE 1691
// MISSING LINE 1692
// MISSING LINE 1693
// MISSING LINE 1694
// MISSING LINE 1695
// MISSING LINE 1696
// MISSING LINE 1697
// MISSING LINE 1698
// MISSING LINE 1699
// MISSING LINE 1700
// MISSING LINE 1701
// MISSING LINE 1702
// MISSING LINE 1703
// MISSING LINE 1704
// MISSING LINE 1705
// MISSING LINE 1706
// MISSING LINE 1707
// MISSING LINE 1708
// MISSING LINE 1709
                        </tbody>\r
                      </table>\r
                    </div>\r
                  </div>
                </div>
              </article>
            </section>
          )}
\r
          {isUserEditorOpen && (\r
\r
            <div\r
              className="user-modal-backdrop"\r
              role="presentation"\r
              onMouseDown={handleCancelUserEdit}\r
            >\r
              <article\r
                className="panel user-modal"\r
                role="dialog"\r
                aria-modal="true"\r
                aria-labelledby="user-modal-title"\r
// MISSING LINE 1731
// MISSING LINE 1732
// MISSING LINE 1733
// MISSING LINE 1734
// MISSING LINE 1735
// MISSING LINE 1736
// MISSING LINE 1737
// MISSING LINE 1738
// MISSING LINE 1739
// MISSING LINE 1740
// MISSING LINE 1741
// MISSING LINE 1742
// MISSING LINE 1743
// MISSING LINE 1744
// MISSING LINE 1745
// MISSING LINE 1746
// MISSING LINE 1747
// MISSING LINE 1748
// MISSING LINE 1749
// MISSING LINE 1750
                      <input\r
                        value={userForm.username}\r
                        onChange={handleUserFieldChange('username')}\r
                        disabled={Boolean(selectedUser) && !creatingUser}\r
                        placeholder="usuario.sistema"\r
                      />\r
                    </label>\r
\r
                    <label>\r
                      <span>Nombre</span>\r
                      <input value={userForm.nombre} onChange={handleUserFieldChange('nombre')} disabled={!canConfigure} placeholder="Nombre completo" />\r
                    </label>\r
\r
                    <label>\r
                      <span>Correo</span>\r
                      <input type="email" value={userForm.correo} onChange={handleUserFieldChange('correo')} disabled={!canConfigure} placeholder="correo@dominio.com" />\r
                    </label>\r
\r
                    <label>\r
                      <span>Dependencia</span>\r
                      <input value={userForm.dependencia} onChange={handleUserFieldChange('dependencia')} disabled={!canConfigure} placeholder="Area o dependencia" />\r
                    </label>\r
\r
                    <label className="span-full">\r
                      <span>Rol</span>\r
                      <select value={userForm.rol} onChange={handleUserFieldChange('rol')} disabled={!canConfigure || roles.length === 0}>\r
                        <option value="">{roles.length === 0 ? 'Sin roles disponibles' : 'Selecciona un rol'}</option>\r
                        {roles.map((role) => (\r
                          <option key={role.codigo} value={role.codigo}>\r
                            {role.nombre} - {role.codigo}\r
                          </option>\r
                        ))}\r
                      </select>\r
                    </label>\r
\r
                    <label className="toggle-field">\r
                      <span>Activo</span>\r
                      <label className="switch">\r
                        <input type="checkbox" checked={Boolean(userForm.activo)} onChange={handleUserFieldChange('activo')} disabled={!canConfigure} />\r
                        <span />\r
                      </label>\r
                    </label>\r
                  </div>\r
\r
                  <div className="form-actions">\r
// MISSING LINE 1796
// MISSING LINE 1797
// MISSING LINE 1798
// MISSING LINE 1799
// MISSING LINE 1800
// MISSING LINE 1801
// MISSING LINE 1802
// MISSING LINE 1803
// MISSING LINE 1804
// MISSING LINE 1805
// MISSING LINE 1806
// MISSING LINE 1807
// MISSING LINE 1808
// MISSING LINE 1809
// MISSING LINE 1810
// MISSING LINE 1811
              </form>\r
            </article>\r
          </div>\r
                      <Save size={16} />\r
                      {savingUser ? 'Guardando...' : 'Guardar usuario'}\r
                    </button>\r
                  )}\r
                </div>\r
              </form>\r
            </article>\r
          </div>\r
          )}\r
        </section>\r
      )}\r
\r
            {activeSection === SECURITY_TABS.ROLES && !hasRoleEditorOpen && (\r
        <section className="security-workspace roles-workspace">\r
          <article className="panel panel-main roles-panel">\r
            <div className="panel-topbar roles-topbar">\r
              <div className="roles-header-copy">\r
                <p className="security-eyebrow">GESTIÃ“N DE ROLES</p>\r
                <h2>GestiÃ³n de Roles</h2>\r
                <p>Administre los niveles de acceso y perfiles de seguridad del sistema.</p>\r
              </div>\r
                  <input\r
                    type="text"\r
                    value={roleSearch}\r
                    onChange={(event) => setRoleSearch(event.target.value)}\r
                    placeholder="Buscar rol"\r
                  />\r
                </div>\r
                {canConfigure && (\r
                  <button type="button" className="btn-primary" onClick={handleNewRole} disabled={loading}>\r
                    <Plus size={16} />\r
                    Nuevo Rol Personalizado\r
                  </button>\r
                )}\r
              </div>\r
            </div>\r
            </div>

            <div className="table-shell roles-table-shell">
              <table className="data-table roles-table">
                <thead>
                  <tr>
                    <th>Nombre del rol</th>
                    <th>Descripción</th>
                    <th>Tipo de perfil</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={5} className="table-empty-cell">Cargando roles...</td>
                    </tr>
                  ) : filteredRoles.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="table-empty-cell">
                        {emptyMessage('No hay roles', 'Crea un rol para empezar a definir su matriz de permisos.')}
                      </td>
                    </tr>
                  ) : (
                    filteredRoles.map((role) => {
                      const selected = role.codigo === selectedRoleCode;
                      return (
                        <tr key={role.codigo} className={selected ? 'selected-row' : ''} onClick={() => handleSelectRole(role)}>
                          <td>
                            <div className="role-table-name">
                              <span className="role-table-badge">
                                <ShieldCheck size={15} />
                              </span>
                              <div>
                                <strong>{role.nombre}</strong>
                                <span>ID: {role.id ? `#${role.id}` : role.codigo}</span>
                              </div>
                            </div>
                          </td>
                          <td className="role-table-description">{role.descripcion || 'Sin descripción'}</td>
                          <td>
                            <span className={`profile-chip ${roleTypeClass(role)}`}>{roleTypeLabel(role)}</span>
                          </td>
                          <td>
                            <span className={`status-chip ${role.activo ? 'active' : 'inactive'}`}>
                              {role.activo ? 'Activo' : 'Inactivo'}
                            </span>
                          </td>
                          <td>
                            <div className="row-actions">
                              <button
                                type="button"
                                className="icon-button"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  handleSelectRole(role);
                                }}
                                title="Editar rol"
                              >
                                <Pencil size={14} />
                              </button>
                              <label className="row-toggle" onClick={(event) => event.stopPropagation()}>
                                <input type="checkbox" checked={Boolean(role.activo)} readOnly />
                                <span />
                              </label>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </article>
        </section>
      )}

            {activeSection === SECURITY_TABS.ROLES && hasRoleEditorOpen && (
        <section className="security-workspace roles-workspace">
          <article className="panel panel-aside role-editor premium-role-editor">
            <form className="editor-form role-editor-form" onSubmit={handleSaveRole}>
              <div className="editor-head role-editor-head">
                <div>
                  <div className="role-editor-title-row">
                    <span className="role-editor-icon">
                      <ShieldCheck size={18} />
                    </span>
                    <div>
                      <h3>{creatingRole ? 'Nuevo Rol Personalizado' : 'Editar Perfil de Seguridad'}</h3>
                      <p>
                        {creatingRole
                          ? 'Defina el perfil y ajuste los privilegios antes de guardar.'
                          : `Modificando privilegios para: ${selectedRole?.nombre || 'Rol seleccionado'}`}
                      </p>
                    </div>
                  </div>
                </div>

                <button type="button" className="btn-ghost-dark" onClick={handleCancelRoleEdit}>
                  <span aria-hidden="true">?</span>
                  Volver al Listado
                </button>
              </div>

              <div className="role-core-grid">
                {creatingRole && (
                  <label className="span-full">
                    <span>Código del rol *</span>
                    <input
                      value={roleForm.codigo}
                      onChange={handleRoleFieldChange('codigo')}
                      disabled={!canConfigure}
                      placeholder="ej: visualizadores_maestro"
                    />
                  </label>
                )}

                <label>
                  <span>Nombre del rol *</span>
                  <input
                    value={roleForm.nombre}
                    onChange={handleRoleFieldChange('nombre')}
                    disabled={!canConfigure}
                    placeholder="Visualizadores Maestro"
                  />
                </label>

                <label>
                  <span>Descripción del perfil</span>
                  <textarea
                    value={roleForm.descripcion}
                    onChange={handleRoleFieldChange('descripcion')}
                    disabled={!canConfigure}
                    placeholder="Pueden ver los estados de las cuentas y revisar índices analíticos"
                    rows={2}
                  />
                </label>
              </div>

              <section className="role-section-card">
                <div className="role-section-header">
                  <div>
                    <h4>Restricciones de Datos y Workflow</h4>
                    <p>Configure el alcance del rol y su comportamiento base dentro de la plataforma.</p>
                  </div>
                </div>

                <div className="role-constraint-grid">
                  <div className="constraint-card">
                    <h5>Visibilidad de Información</h5>
                    <div className="constraint-list">
                      <label className="constraint-item" title={getConstraintTooltip('transversal')}>
                        <input type="checkbox" checked={Boolean(roleForm.transversal)} onChange={handleRoleFieldChange('transversal')} />
                        <span>Privacidad Estricta</span>
                      </label>
                      <label className="constraint-item" title={getConstraintTooltip('activo')}>
                        <input type="checkbox" checked={Boolean(roleForm.activo)} onChange={handleRoleFieldChange('activo')} />
                        <span>Rol Activo</span>
                      </label>
                      <label className="constraint-item" title={getConstraintTooltip('access')}>
                        <input type="checkbox" checked={Boolean(selectedRole?.transversal)} readOnly disabled />
                        <span>Acceso Transversal</span>
                      </label>
                    </div>
                  </div>

                  <div className="constraint-card">
                    <h5>Acceso a Bloques</h5>
                    {creatingRole && (
                      <div className="template-strip template-strip-role">
                        <label>
                          <span>Copiar permisos desde</span>
                          <select value={roleTemplateCode} onChange={(event) => handleRoleTemplateChange(event.target.value)} disabled={!canConfigure || roles.length === 0}>
                            <option value="">{roles.length === 0 ? 'Sin roles para copiar' : 'Sin plantilla'}</option>
                            {roles.map((role) => (
                              <option key={role.codigo} value={role.codigo}>
                                {role.nombre} - {role.codigo}
                              </option>
                            ))}
                          </select>
                        </label>
                      </div>
                    )}

                    <div className="workflow-block-grid">
                        {permissionGroups.slice(0, 5).map((group) => {
                          const codes = getBucketCodes(group, 'visualizar');
                          const checked = isBucketChecked(codes);
                          return (
                          <label
                            key={group.key}
                            className={`workflow-block ${checked ? 'checked' : ''}`}
                            title={getMatrixCellTitle(group, 'visualizar', codes)}
                          >
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => togglePermissionBucket(codes)}
                            />
                            <span className="workflow-block-copy">{group.label}</span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </section>

              <section className="role-section-card">
                <div className="role-section-header">
                  <div>
                    <h4>Responsabilidades de Procesamiento</h4>
                    <p>Seleccione los módulos donde este rol actuará como operador directo.</p>
                  </div>
                </div>

                <div className="workflow-responsibility-grid">
                  {permissionGroups.map((group) => {
                    const allCodes = group.permissions.map((permission) => permission.codigo);
                    const checked = isBucketChecked(allCodes);
                    return (
                      <label
                        key={group.key}
                        className={`responsibility-card ${checked ? 'checked' : ''}`}
                        title={`${group.label}: habilita permisos de operación directa sobre este módulo.`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => togglePermissionBucket(allCodes)}
                        />
                        <span className="responsibility-copy">{group.label}</span>
                      </label>
                    );
                  })}
                </div>
              </section>

              <section className="role-section-card matrix-section-card">
                <div className="matrix-head">
                  <div>
                    <h4>Matriz de Privilegios del Sistema</h4>
                    <p>Controle qué acciones puede ejecutar este perfil sobre cada módulo funcional.</p>
                  </div>
                  <span className="soft-pill">{permissionGroups.length} módulos</span>
                </div>

                <div className="matrix-table-shell">
                  <table className="matrix-table">
                    <thead>
                      <tr>
                        <th title="Módulo o funcionalidad a la que aplican los permisos.">Módulo / Funcionalidad</th>
                        <th title={`${matrixColumnDescriptions.visualizar} ${matrixColumnShortcuts.visualizar}`}>Visualizar</th>
                        <th title={`${matrixColumnDescriptions.crear} ${matrixColumnShortcuts.crear}`}>Crear</th>
                        <th title={`${matrixColumnDescriptions.editar} ${matrixColumnShortcuts.editar}`}>Editar</th>
                        <th title={`${matrixColumnDescriptions.especiales} ${matrixColumnShortcuts.especiales}`}>Especiales</th>
                      </tr>
                    </thead>
                    <tbody>
                      {permissionGroups.map((group) => {
                        const viewCodes = getBucketCodes(group, 'visualizar');
                        const createCodes = getBucketCodes(group, 'crear');
                        const editCodes = getBucketCodes(group, 'editar');
                        const specialCodes = getBucketCodes(group, 'especiales');
                        const cells = [
                          { key: 'visualizar', codes: viewCodes },
                          { key: 'crear', codes: createCodes },
                          { key: 'editar', codes: editCodes },
                          { key: 'especiales', codes: specialCodes },
                        ];

                        return (
                          <tr key={group.key}>
                            <td>
                              <div
                                className="matrix-module-cell"
                                title={group.permissions.map((permission) => getPermissionTooltip(permission)).join(' · ')}
                              >
                                <span className="matrix-module-icon">
                                  <ShieldCheck size={15} />
                                </span>
                                <div>
                                  <strong>{group.label}</strong>
                                  <span>{group.key}</span>
                                </div>
                              </div>
                            </td>
                            {cells.map((cell) => {
                              const checked = isBucketChecked(cell.codes);
                              const cellTitle = getMatrixCellTitle(group, cell.key, cell.codes);
                              return (
                                <td key={`${group.key}-${cell.key}`} className="matrix-center-cell" title={cellTitle}>
                                  {cell.codes.length === 0 ? (
                                    <span className="muted-text" title={cellTitle}>N/A</span>
                                  ) : (
                                    <label className={`matrix-toggle ${checked ? 'checked' : ''}`} title={cellTitle}>
                                      <input
                                        type="checkbox"
                                        checked={checked}
                                        onChange={() => togglePermissionBucket(cell.codes)}
                                      />
                                      <span />
                                    </label>
                                  )}
                                </td>
                              );
                            })}
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <div className="form-actions sticky-actions">
                  <button type="button" className="btn-secondary" onClick={handleCancelRoleEdit} disabled={savingRole || savingPermissions}>
                    Cancelar
                  </button>
                  {selectedRole && !creatingRole && (
                    <button type="button" className="btn-ghost-danger" onClick={handleDeleteRole} disabled={savingRole || savingPermissions || !canConfigure}>
                      <Ban size={16} />
                      Desactivar
                    </button>
                  )}
                  <button type="submit" className="btn-primary" disabled={savingRole || savingPermissions || !canConfigure}>
                    <Save size={16} />
                    {savingRole || savingPermissions ? 'Guardando...' : creatingRole ? 'Crear rol' : 'Guardar rol'}
                  </button>
                </div>
              </section>
            </form>
          </article>
        </section>
      )}
    </div>
  );
};

export default SecurityConfigPage;

