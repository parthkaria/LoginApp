(function() {
    'use strict';

    angular
        .module('loginappApp')
        .config(localStorageConfig);

    localStorageConfig.$inject = ['$localStorageProvider', '$sessionStorageProvider'];

    function localStorageConfig($localStorageProvider, $sessionStorageProvider) {
        $localStorageProvider.setKeyPrefix('app-');
        $sessionStorageProvider.setKeyPrefix('app-');
    }
})();
